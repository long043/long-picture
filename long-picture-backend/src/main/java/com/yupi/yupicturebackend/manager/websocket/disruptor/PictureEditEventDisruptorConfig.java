package com.yupi.yupicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 定义 Disruptor؜ 配置类，创建Disruptor实例，把刚定义的事件和事件处理器关联到 Disruptor 实例中
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;

        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                //创建 PictureEditEvent 类型的事件
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );

        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动 disruptor，使其开始接收和处理事件。
        disruptor.start();
        //将创建并配置好的 Disruptor 实例作为Bean（bean 名称为 “pictureEditEventDisruptor”）返回，这样其他组件就可通过依赖注入来使用这个 Disruptor 实例发送和处理图片编辑事件。
        return disruptor;
    }
}
