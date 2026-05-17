package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.service.AiEnhancementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 图片清晰化服务实现
 */
@Slf4j
@Service
public class AiEnhancementServiceImpl implements AiEnhancementService {

    private static final String DEFAULT_API_URL = "https://e8c79f6b588015f8ba.gradio.live/sdapi/v1/img2img";

    private static final Duration AI_REQUEST_TIMEOUT = Duration.ofSeconds(600);

    private static final String PROMPT_PREFIX = "masterpiece, best quality, highly detailed, sharp focus, crisp details, natural texture";

    private static final String PROMPT_SUFFIX = "<lora:Long_general_detail_enhancer_v1:0.8>";

    private static final String NEGATIVE_PROMPT = "blurry, low quality, worst quality, low resolution, out of focus, over-smoothed, jpeg artifacts, noise, text, watermark, logo";

    private static final String SAMPLER_NAME = "DPM++ 2M";

    private static final String SCHEDULER = "Karras";

    private static final String CONTROLNET_MODULE = "tile_resample";

    private static final String CONTROLNET_MODEL = "control_v11f1e_sd15_tile [a371b31b]";

    private static final String UPSCALE_SCRIPT_NAME = "ultimate sd upscale";

    private static final int UPSCALER_4X_ULTRASHARP_INDEX = 3;

    private static final int STEPS = 28;

    private static final double CFG_SCALE = 7.0;

    private static final double DENOISING_STRENGTH = 0.25;

    private static final int TILE_SIZE = 512;

    private static final int MASK_BLUR = 8;

    private static final int PADDING = 32;

    private static final int CONTROLNET_PROCESSOR_RES = 768;

    private static final double UPSCALE_FACTOR = 2.0;

    @Value("${ai.sd.api-url:" + DEFAULT_API_URL + "}")
    private String apiUrl = DEFAULT_API_URL;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String processImage(String base64Image) {
        return processImage(base64Image, null, null);
    }

    @Override
    public String processImage(String base64Image, Integer width, Integer height) {
        if (StrUtil.isBlank(base64Image)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        String pureBase64Image = removeDataUriPrefix(base64Image);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(AI_REQUEST_TIMEOUT)
                .build();
        String caption = tryInterrogateImage(httpClient, pureBase64Image);
        String requestBody = buildSdPayload(pureBase64Image, width, height, caption);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildImg2ImgUrl()))
                .timeout(AI_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI 清晰接口调用失败，statusCode = {}, body = {}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰失败：" + extractSdErrorMessage(response.body()));
            }
            return parseEnhancedImage(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI 清晰请求被中断", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰请求被中断");
        } catch (HttpTimeoutException e) {
            log.error("AI 清晰接口调用超时，url = {}", buildImg2ImgUrl(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰处理超时，请稍后重试或处理更小的图片");
        } catch (IOException e) {
            log.error("AI 清晰接口连接失败，url = {}", buildImg2ImgUrl(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, buildConnectionErrorMessage(e));
        }
    }

    private String tryInterrogateImage(HttpClient httpClient, String base64Image) {
        String requestBody = buildInterrogatePayload(base64Image);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildInterrogateUrl()))
                .timeout(AI_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI 图片反推接口调用失败，statusCode = {}, body = {}", response.statusCode(), response.body());
                return null;
            }
            return parseInterrogateCaption(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI 图片反推请求被中断", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片反推请求被中断");
        } catch (BusinessException e) {
            log.warn("AI 图片反推失败，将使用默认清晰化提示词：{}", e.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("AI 图片反推接口调用异常，将使用默认清晰化提示词", e);
            return null;
        }
    }

    private String buildInterrogatePayload(String base64Image) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("image", base64Image);
        payload.put("model", "clip");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("构造 AI 图片反推请求体失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "构造 AI 图片反推请求体失败");
        }
    }

    private String buildInterrogateUrl() {
        return buildApiUrl("/sdapi/v1/interrogate");
    }

    private String buildImg2ImgUrl() {
        return buildApiUrl("/sdapi/v1/img2img");
    }

    private String buildApiUrl(String path) {
        String normalizedApiUrl = StrUtil.removeSuffix(apiUrl, "/");
        int apiPathIndex = normalizedApiUrl.indexOf("/sdapi/v1/");
        if (apiPathIndex >= 0) {
            return normalizedApiUrl.substring(0, apiPathIndex) + path;
        }
        return normalizedApiUrl + path;
    }


    private String parseInterrogateCaption(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode captionNode = rootNode.get("caption");
            if (captionNode == null || !captionNode.isTextual() || StrUtil.isBlank(captionNode.asText())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片反推接口未返回描述");
            }
            return captionNode.asText().trim();
        } catch (JsonProcessingException e) {
            log.error("AI 图片反推接口响应解析失败，body = {}", responseBody, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片反推接口响应解析失败");
        }
    }

    private String extractSdErrorMessage(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "模型服务未返回错误详情";
        }
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String message = firstText(rootNode, "errors", "detail", "error", "message");
            if (StrUtil.isNotBlank(message)) {
                return truncateMessage(message);
            }
        } catch (JsonProcessingException e) {
            log.warn("AI 清晰接口错误响应解析失败，body = {}", responseBody, e);
        }
        return truncateMessage(responseBody);
    }

    private String firstText(JsonNode rootNode, String... fields) {
        if (rootNode == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode valueNode = rootNode.get(field);
            if (valueNode != null && !valueNode.isNull()) {
                String value = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String truncateMessage(String message) {
        if (StrUtil.isBlank(message)) {
            return "模型服务调用失败";
        }
        int maxLength = 300;
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
    }

    private String buildConnectionErrorMessage(IOException e) {
        String reason = StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName());
        String targetUrl = buildImg2ImgUrl();
        if (targetUrl.contains("127.0.0.1") || targetUrl.contains("localhost")) {
            return "AI 清晰接口连接失败，请确认本机 Stable Diffusion WebUI 已启动，或配置 SD_WEBUI_IMG2IMG_URL 为云端地址";
        }
        return "AI 清晰接口连接失败，请检查 Stable Diffusion WebUI 地址是否可访问：" + truncateMessage(reason);
    }

    private String parseEnhancedImage(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode imagesNode = rootNode.get("images");
            if (imagesNode == null || !imagesNode.isArray() || imagesNode.size() == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰接口未返回图片");
            }
            JsonNode firstImageNode = imagesNode.get(0);
            if (firstImageNode == null || !firstImageNode.isTextual() || StrUtil.isBlank(firstImageNode.asText())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰接口返回图片为空");
            }
            return firstImageNode.asText();
        } catch (JsonProcessingException e) {
            log.error("AI 清晰接口响应解析失败，body = {}", responseBody, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰接口响应解析失败");
        }
    }

    private String buildSdPayload(String base64Image) {
        return buildSdPayload(base64Image, null, null, null);
    }

    private String buildSdPayload(String base64Image, Integer width, Integer height, String caption) {
        ImageSize imageSize = resolveImageSize(base64Image, width, height);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("init_images", Collections.singletonList(base64Image));
        payload.put("prompt", buildFinalPrompt(caption));
        payload.put("negative_prompt", NEGATIVE_PROMPT);
        payload.put("sampler_name", SAMPLER_NAME);
        payload.put("scheduler", SCHEDULER);
        payload.put("steps", STEPS);
        payload.put("cfg_scale", CFG_SCALE);
        payload.put("denoising_strength", DENOISING_STRENGTH);
        payload.put("width", imageSize.width);
        payload.put("height", imageSize.height);
        payload.put("batch_size", 1);
        payload.put("n_iter", 1);
        payload.put("send_images", true);
        payload.put("save_images", false);
        payload.put("do_not_save_samples", true);
        payload.put("do_not_save_grid", true);

        Map<String, Object> controlNetArg = new LinkedHashMap<>();
        controlNetArg.put("enabled", true);
        controlNetArg.put("pixel_perfect", true);
        controlNetArg.put("image", base64Image);
        controlNetArg.put("module", CONTROLNET_MODULE);
        controlNetArg.put("model", CONTROLNET_MODEL);
        controlNetArg.put("weight", 1.0);
        controlNetArg.put("resize_mode", "Crop and Resize");
        controlNetArg.put("processor_res", CONTROLNET_PROCESSOR_RES);
        controlNetArg.put("guidance_start", 0.0);
        controlNetArg.put("guidance_end", 1.0);
        controlNetArg.put("control_mode", "ControlNet is more important");
        controlNetArg.put("save_detected_map", false);

        Map<String, Object> controlNet = new LinkedHashMap<>();
        controlNet.put("args", Collections.singletonList(controlNetArg));

        Map<String, Object> alwaysOnScripts = new LinkedHashMap<>();
        alwaysOnScripts.put("controlnet", controlNet);
        payload.put("alwayson_scripts", alwaysOnScripts);
        payload.put("script_name", UPSCALE_SCRIPT_NAME);
        payload.put("script_args", buildUltimateSdUpscaleArgs());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("构造 AI 清晰请求体失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "构造 AI 清晰请求体失败");
        }
    }

    private String buildFinalPrompt(String caption) {
        if (StrUtil.isBlank(caption)) {
            return PROMPT_PREFIX + ", " + PROMPT_SUFFIX;
        }
        return PROMPT_PREFIX + ", " + truncateCaption(caption.trim()) + ", " + PROMPT_SUFFIX;
    }

    private Object[] buildUltimateSdUpscaleArgs() {
        return new Object[]{
                "",
                TILE_SIZE,
                TILE_SIZE,
                MASK_BLUR,
                PADDING,
                64,
                DENOISING_STRENGTH,
                PADDING,
                UPSCALER_4X_ULTRASHARP_INDEX,
                true,
                0,
                false,
                MASK_BLUR,
                0,
                2,
                2048,
                2048,
                UPSCALE_FACTOR
        };
    }

    private String truncateCaption(String caption) {
        int maxLength = 350;
        if (caption.length() <= maxLength) {
            return caption;
        }
        return caption.substring(0, maxLength);
    }

    private ImageSize resolveImageSize(String base64Image, Integer width, Integer height) {
        if (width != null && width > 0 && height != null && height > 0) {
            return new ImageSize(normalizeSdDimension(width), normalizeSdDimension(height));
        }
        return getImageSize(base64Image);
    }

    private ImageSize getImageSize(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(removeDataUriPrefix(base64Image));
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式错误，无法解析宽高");
            }
            return new ImageSize(normalizeSdDimension(bufferedImage.getWidth()), normalizeSdDimension(bufferedImage.getHeight()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 Base64 格式错误");
        } catch (IOException e) {
            log.error("解析图片宽高失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析图片宽高失败");
        }
    }

    private int normalizeSdDimension(int dimension) {
        return Math.max(64, ((dimension + 7) / 8) * 8);
    }

    private String removeDataUriPrefix(String base64Image) {
        if (StrUtil.isBlank(base64Image)) {
            return base64Image;
        }
        int commaIndex = base64Image.indexOf(',');
        if (base64Image.startsWith("data:") && commaIndex >= 0) {
            return base64Image.substring(commaIndex + 1);
        }
        return base64Image;
    }

    private static class ImageSize {

        private final int width;

        private final int height;

        private ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
