package com.yupi.yupicturebackend.manager.auth.annotation;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

/**
 * Sa-Token 开启注解和配置
 */
//标记这是一个Spring配置类，Spring启动时会自动加载并解析其中的配置逻辑。
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    public void addInterceptors(InterceptorRegistry registry) {
        //.addPathPatterns("/**")：配置拦截规则，"/**" 表示拦截所有请求
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }


    //由于使用了多账号体系，؜每次使用注解时都要指定账号体系的loginType，会比较麻烦，进行注解合并
    @PostConstruct
    public void rewriteSaStrategy() {
        // 重写Sa-Token的注解处理器，增加注解合并功能 
        SaAnnotationStrategy.instance.getAnnotation = (element, annotationClass) -> {
            return AnnotatedElementUtils.getMergedAnnotation(element, annotationClass);
        };
    }
}