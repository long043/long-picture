package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 * 把处理websocket消息需要的参数封装到事件类中，由生产者发送到队列中，让消费者消费。
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}