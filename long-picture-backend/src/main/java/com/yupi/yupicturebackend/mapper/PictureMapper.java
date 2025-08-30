package com.yupi.yupicturebackend.mapper;

import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 李鱼皮
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2024-12-11 20:45:51
* @Entity com.yupi.yupicturebackend.model.entity.Picture
*/
// MyBatis-Plus 的 BaseMapper 继承机制，替代了 @Mapper 的功能。所以这里没有mapper注解
public interface PictureMapper extends BaseMapper<Picture> {

}




