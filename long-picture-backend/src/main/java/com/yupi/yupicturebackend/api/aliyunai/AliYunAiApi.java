package com.yupi.yupicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey:}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    private static final int HTTP_TIMEOUT = 30000;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 200;


    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        checkApiKey();
        // 通过Hutool的HTTP请求工具类构造一个HTTP请求。然后发送请求，调用阿里云百炼的API
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .timeout(HTTP_TIMEOUT)
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        // 处理响应。.execute() 方法是触发 HTTP 请求发送的核心，它会实际向阿里云百炼的 API 接口发起调用并获取返回结果。
        try (HttpResponse httpResponse = httpRequest.execute()) {
            String responseBody = httpResponse.body();
            if (!httpResponse.isOk()) {
                log.error("创建 AI 扩图任务失败，status = {}, body = {}", httpResponse.getStatus(), responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败，" + getErrorMessage(responseBody));
            }
            //将响应体转换为java响应对象
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
            //API官方文档中，有code值是错误。如果成功是不返回这个值的
            if (createOutPaintingTaskResponse == null) {
                log.error("创建 AI 扩图任务响应为空，body = {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应为空");
            }
            if (StrUtil.isNotBlank(createOutPaintingTaskResponse.getCode())) {
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败，" + errorMessage);
            }
            if (createOutPaintingTaskResponse.getOutput() == null
                    || StrUtil.isBlank(createOutPaintingTaskResponse.getOutput().getTaskId())) {
                log.error("创建 AI 扩图任务未返回任务 ID，body = {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口未返回任务 ID");
            }
            return createOutPaintingTaskResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建 AI 扩图任务异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口调用失败，请稍后重试");
        }
    }

    /**
     * 查询创建的任务结果
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        checkApiKey();
        // 处理响应
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(HTTP_TIMEOUT)
                .execute()) {
            String responseBody = httpResponse.body();
            if (!httpResponse.isOk()) {
                log.error("获取 AI 扩图任务结果失败，status = {}, body = {}", httpResponse.getStatus(), responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败，" + getErrorMessage(responseBody));
            }
            GetOutPaintingTaskResponse taskResponse = JSONUtil.toBean(responseBody, GetOutPaintingTaskResponse.class);
            if (taskResponse == null) {
                log.error("获取 AI 扩图任务结果响应异常，body = {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图任务状态异常");
            }
            if (StrUtil.isNotBlank(taskResponse.getCode())) {
                log.error("获取 AI 扩图任务结果异常，code = {}, message = {}", taskResponse.getCode(), taskResponse.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败，" + taskResponse.getMessage());
            }
            if (taskResponse.getOutput() == null || StrUtil.isBlank(taskResponse.getOutput().getTaskStatus())) {
                log.error("获取 AI 扩图任务结果响应异常，body = {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图任务状态异常");
            }
            return taskResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 AI 扩图任务结果异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败，请稍后重试");
        }
    }

    private void checkApiKey() {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请先配置 aliYunAi.apiKey");
        }
    }

    private String getErrorMessage(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "阿里云接口无响应内容";
        }
        try {
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            String message = responseJson.getStr("message");
            String code = responseJson.getStr("code");
            JSONObject output = responseJson.getJSONObject("output");
            if (StrUtil.isBlank(message) && output != null) {
                message = output.getStr("message");
            }
            if (StrUtil.isBlank(code) && output != null) {
                code = output.getStr("code");
            }
            if (StrUtil.isNotBlank(message)) {
                return StrUtil.isBlank(code) ? truncate(message) : code + "：" + truncate(message);
            }
        } catch (Exception e) {
            log.warn("解析阿里云 AI 错误响应失败，body = {}", responseBody);
        }
        return truncate(responseBody);
    }

    private String truncate(String message) {
        if (StrUtil.isBlank(message)) {
            return "未知错误";
        }
        return StrUtil.maxLength(message, MAX_ERROR_MESSAGE_LENGTH);
    }
}
