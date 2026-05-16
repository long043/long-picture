package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 清晰请求
 */
@Data
public class ImageEnhanceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 纯 Base64 图片字符串，不包含 data:image 前缀
     */
    private String image;

    /**
     * 图片 id，用于处理当前已上传的图片
     */
    private Long pictureId;

    /**
     * 图片真实宽度，直接传 image 时可选
     */
    private Integer width;

    /**
     * 图片真实高度，直接传 image 时可选
     */
    private Integer height;
}
