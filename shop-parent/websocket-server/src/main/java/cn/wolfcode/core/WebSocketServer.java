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

@ServerEndpoint("/{token}")
@Slf4j
@Component
public class WebSocketServer {
    public static ConcurrentHashMap<String,Session> SESSION_MAP=new ConcurrentHashMap<>();
    @OnOpen
    public void OnOpen(Session session,@PathParam("token") String token){
        log.info("连接建立成功，{}",token);
        SESSION_MAP.put(token,session);
    }
    @OnClose
    public void OnClose(@PathParam("token") String token){
        log.info("连接断开");
        SESSION_MAP.remove(token);
    }
    @OnError
    public void OnError(Throwable throwable){
        log.warn("连接异常："+throwable);
    }

}
