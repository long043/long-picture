package com.yupi.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员权限配置，对应 总的权限配置文件（包括权限和角色） 的实体类
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}