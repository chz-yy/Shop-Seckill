package cn.wolfcode.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaoliu
 * @date 2023/6/10
 */
@ServerEndpoint("/ws/{token}")
@Component
@Slf4j
public class WebSocketServer {

    public static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        log.info("[WebSocket 服务] 接收新的客户连接：{}", token);
        SESSION_MAP.put(token, session);
    }

    @OnMessage
    public void onMessage(@PathParam("token") String token, String message) {
        log.info("[WebSocket 服务] 收到新消息 token：{}, message: {}", token, message);
    }

    @OnClose
    public void onClose(@PathParam("token") String token) {
        log.info("[WebSocket 服务] 客户端下线了：{}", token);
        SESSION_MAP.remove(token);
    }

    @OnError
    public void onError(@PathParam("token") String token, Throwable throwable) {
        log.info("[WebSocket 服务] {}，连接出现异常", token, throwable);
    }
}
