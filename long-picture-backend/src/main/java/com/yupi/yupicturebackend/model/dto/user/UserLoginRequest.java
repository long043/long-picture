package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
//实现序列化：更方便在网络中传输，或有时需要序列化
public class UserLoginRequest implements Serializable {

    //序列化ID
    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
