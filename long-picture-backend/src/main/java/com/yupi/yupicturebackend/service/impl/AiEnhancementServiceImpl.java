package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.service.AiEnhancementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 图片清晰化服务实现
 */
@Slf4j
@Service
public class AiEnhancementServiceImpl implements AiEnhancementService {

    private static final String API_URL = "https://xxxxxx.gradio.live/sdapi/v1/img2img";

    private static final Duration AI_REQUEST_TIMEOUT = Duration.ofSeconds(180);

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String processImage(String base64Image) {
        if (StrUtil.isBlank(base64Image)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        if (base64Image.startsWith("data:")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请传入不带 data:image 前缀的纯 Base64 字符串");
        }
        String requestBody = buildSdPayload(base64Image);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(AI_REQUEST_TIMEOUT)
                .build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(AI_REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI 清晰接口调用失败，statusCode = {}, body = {}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰失败");
            }
            return parseEnhancedImage(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI 清晰请求被中断", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰请求被中断");
        } catch (IOException e) {
            log.error("AI 清晰接口调用异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 清晰接口调用异常");
        }
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("init_images", Collections.singletonList(base64Image));
        payload.put("prompt", "masterpiece, best quality, highly detailed natural landscape, photorealistic, sharp focus");
        payload.put("negative_prompt", "blurry, low quality, deformed, worst quality");
        payload.put("denoising_strength", 0.25);
        payload.put("width", 512);
        payload.put("height", 512);
        payload.put("script_name", "ultimate sd upscale");

        List<Object> scriptArgs = new ArrayList<>();
        Collections.addAll(scriptArgs, null, 512, 512, 8, 32, 64, 0.35, 32, 0, true, 0, false, 8, 0, 2, 2.0, "4x-UltraSharp");
        payload.put("script_args", scriptArgs);

        Map<String, Object> controlNetArg = new LinkedHashMap<>();
        controlNetArg.put("input_image", base64Image);
        controlNetArg.put("module", "tile_resample");
        controlNetArg.put("model", "control_v11f1e_sd15_tile [a23b2b17]");
        controlNetArg.put("weight", 1.0);
        controlNetArg.put("resize_mode", 1);

        Map<String, Object> controlNet = new LinkedHashMap<>();
        controlNet.put("args", Collections.singletonList(controlNetArg));

        Map<String, Object> alwaysOnScripts = new LinkedHashMap<>();
        alwaysOnScripts.put("controlnet", controlNet);
        payload.put("alwayson_scripts", alwaysOnScripts);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("构造 AI 清晰请求体失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "构造 AI 清晰请求体失败");
        }
    }
}
