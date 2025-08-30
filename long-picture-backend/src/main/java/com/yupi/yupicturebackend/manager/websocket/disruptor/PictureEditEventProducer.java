package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 图片编辑事件生产者
 */
@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布事件
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        // 获取到可以放置事件的下一个位置
        long next = ringBuffer.next();
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        //设置事件内容（封装业务数据）
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机
     *
     * @PreDestroy：
     * 当 Spring 容器关闭时（比如应用停机、重启），会自动调用标记 @PreDestroy 的方法，保证 Bean 在销毁前完成必要的清理操作。
     * 触发时机：应用正常关闭（调用 System.exit() 或容器停止）
     *         开发环境重启、重新部署
     *         云原生场景下的 Pod 重启 / 销毁
     *
     *
     * shutdown()确保：
     * 所有已入队的事件被完整消费（不会丢失事件）
     * 所有 Disruptor 线程（生产者 / 消费者）被优雅终止（不会强制中断导致异常）
     * 释放 Disruptor 占用的资源（如线程池、环形缓冲区等）
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}

