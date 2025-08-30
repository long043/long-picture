package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    //java 序列化机制的版本标识，作用是保障对象在序列化（转成字节流）和反序列化（字节流转回对象）时的兼容性
    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

}
