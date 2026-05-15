package com.yupi.yupicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.dto.picture.ImageEnhanceRequest;
import com.yupi.yupicturebackend.model.vo.ImageEnhanceResponse;
import com.yupi.yupicturebackend.service.AiEnhancementService;
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

    /**
     * AI 清晰
     */
    @PostMapping("/enhance")
    public ImageEnhanceResponse enhanceImage(@RequestBody ImageEnhanceRequest imageEnhanceRequest,
                                             HttpServletRequest request) {
        try {
            userService.getLoginUser(request);
            if (imageEnhanceRequest == null || StrUtil.isBlank(imageEnhanceRequest.getImage())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空");
            }
            String enhancedImage = aiEnhancementService.processImage(imageEnhanceRequest.getImage());
            return ImageEnhanceResponse.success(enhancedImage);
        } catch (BusinessException e) {
            log.error("AI 清晰失败", e);
            return ImageEnhanceResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("AI 清晰失败", e);
            return ImageEnhanceResponse.fail("AI 清晰失败，请稍后重试");
        }
    }
}
