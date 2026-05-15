package com.yupi.yupicturebackend.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 清晰响应
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageEnhanceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    /**
     * 处理后的纯 Base64 图片字符串
     */
    private String enhancedImage;

    /**
     * 错误信息
     */
    private String error;

    public static ImageEnhanceResponse success(String enhancedImage) {
        ImageEnhanceResponse response = new ImageEnhanceResponse();
        response.setSuccess(true);
        response.setEnhancedImage(enhancedImage);
        return response;
    }

    public static ImageEnhanceResponse fail(String error) {
        ImageEnhanceResponse response = new ImageEnhanceResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
}
