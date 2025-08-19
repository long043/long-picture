package com.yupi.yupicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//@Target：限定 AuthCheck 注解只能用在 “方法” 上 。
//@Retention： 指定注解的保留策略，决定了自定义注解在程序的哪个阶段（编译、运行等）保持有效，
// 这里 RetentionPolicy.RUNTIME 表示该注解在程序运行时依然存在，能被反射机制读取到。
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具有某个角色
     **/
    String mustRole() default "";
}
