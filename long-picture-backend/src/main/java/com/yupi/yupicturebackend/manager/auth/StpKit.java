package com.yupi.yupicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，将项目中所有账号体系都定义到这里。比如有用户表体系和团队空间体系
 * 下面定义一个默认，一个space。区分两套鉴权体系。默认的那个不使用，还用第一阶段的校验逻辑
 *
 * StpLogic：是 Sa-Token 框架的核心功能类
 * StpKit：是对 StpLogic 的封装工具类
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     * space是变量名，StpLogic是修饰space的
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}