package com.yupi.yupicturebackend.service;

/**
 * AI 图片清晰化服务
 */
public interface AiEnhancementService {

    /**
     * 处理图片并返回清晰化后的纯 Base64 字符串
     *
     * @param base64Image 纯 Base64 图片字符串
     * @return 清晰化后的纯 Base64 图片字符串
     */
    String processImage(String base64Image);

    /**
     * 按指定宽高处理图片并返回清晰化后的纯 Base64 字符串
     *
     * @param base64Image 纯 Base64 图片字符串
     * @param width       图片真实宽度
     * @param height      图片真实高度
     * @return 清晰化后的纯 Base64 图片字符串
     */
    String processImage(String base64Image, Integer width, Integer height);
}
