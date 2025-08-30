package com.yupi.yupicturebackend.manager.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这是为 “团队空间权限校验” 自定义注解，与其他体系（如普通用户）互不干扰。
 */
//当前注解@SaSpaceCheckPermission继承了Sa-Token框架的@SaCheckPermission注解，并固定type属性为space.
@SaCheckPermission(type = StpKit.SPACE_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
//使用@SaSpaceCheckPermission时，无需手动指定 type，它会自动关联到 “团队空间” 的权限校验逻辑
public @interface SaSpaceCheckPermission {

    /**
     * 需要校验的权限码
     * @return 需要校验的权限码
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] value() default {};

    /**
     * 验证模式：AND | OR，默认AND
     * @return 验证模式
     */
    @AliasFor(annotation = SaCheckPermission.class)
    SaMode mode() default SaMode.AND;

    /**
     * 在权限校验不通过时的次要选择，两者只要其一校验成功即可通过校验
     *
     * <p>
     * 例1：@SaCheckPermission(value="user-add", orRole="admin")，
     * 代表本次请求只要具有 user-add权限 或 admin角色 其一即可通过校验。
     * </p>
     *
     * <p>
     * 例2： orRole = {"admin", "manager", "staff"}，具有三个角色其一即可。 <br>
     * 例3： orRole = {"admin, manager, staff"}，必须三个角色同时具备。
     * </p>
     *
     * @return /
     */
    @AliasFor(annotation = SaCheckPermission.class)
    String[] orRole() default {};

}
