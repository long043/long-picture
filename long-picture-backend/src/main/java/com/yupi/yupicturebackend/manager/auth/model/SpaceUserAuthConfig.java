package com.yupi.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员权限配置，对应总的权限配置文件（包括权限和角色）的实体类
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

/**
 * @Data注解：Lombok 工具库提供的一个核心注解。
 * 作用：在编译阶段自动为被注解的类生成一系列常用的样板代码，具体包括：
 * getter/setter 方法：用于访问和修改类的私有成员变量；
 * toString 方法：用于打印对象的字符串表示，方便调试；
 * equals 和 hashCode 方法：用于对象之间的相等性判断和哈希值计算；
 * 无参构造函数（若类中没有显式定义构造函数时）。
 */