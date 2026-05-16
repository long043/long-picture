package com.yupi.yupicturebackend.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.model.dto.picture.ImageEnhanceRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.vo.ImageEnhanceResponse;
import com.yupi.yupicturebackend.service.AiEnhancementService;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * AI 图片清晰化接口
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class ImageEnhanceController {

    @Resource
    private AiEnhancementService aiEnhancementService;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * AI 清晰
     */
    @PostMapping("/enhance")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public ImageEnhanceResponse enhanceImage(@RequestBody ImageEnhanceRequest imageEnhanceRequest,
                                             HttpServletRequest request) {
        try {
            userService.getLoginUser(request);
            if (imageEnhanceRequest == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
            }
            String image = imageEnhanceRequest.getImage();
            Integer width = imageEnhanceRequest.getWidth();
            Integer height = imageEnhanceRequest.getHeight();
            if (StrUtil.isBlank(image)) {
                Picture picture = getPicture(imageEnhanceRequest.getPictureId());
                image = getPictureBase64(picture);
                // 不信任数据库中的历史宽高，旧数据可能记录的是缩略图尺寸。
                // 交给 AI 服务从实际下载的图片字节解析宽高，避免原图被按 256x256 等小尺寸送入模型。
                width = null;
                height = null;
            }
            String enhancedImage = aiEnhancementService.processImage(image, width, height);
            return ImageEnhanceResponse.success(enhancedImage);
        } catch (BusinessException e) {
            log.error("AI 清晰失败", e);
            return ImageEnhanceResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("AI 清晰失败", e);
            return ImageEnhanceResponse.fail("AI 清晰失败，请稍后重试");
        }
    }

    /**
     * 获取当前图片的纯 Base64 内容
     */
    private String getPictureBase64(Picture picture) {
        String pictureUrl = picture.getUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(pictureUrl), ErrorCode.PARAMS_ERROR, "图片地址为空");
        byte[] imageBytes = HttpUtil.downloadBytes(pictureUrl);
        ThrowUtils.throwIf(imageBytes == null || imageBytes.length == 0, ErrorCode.OPERATION_ERROR, "图片下载失败");
        return Base64.encode(imageBytes);
    }

    private Picture getPicture(Long pictureId) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
        }
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picture;
    }
}
