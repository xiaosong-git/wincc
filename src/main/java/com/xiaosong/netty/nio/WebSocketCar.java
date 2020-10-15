package com.xiaosong.netty.nio;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketCar {
    private static Map<String, Channel> carNameWebsession = new ConcurrentHashMap<>();

    public static void add(String carName, Channel webSocketSession) {
        carNameWebsession.put(carName,webSocketSession );
    }

    /**
     * 根据车牌号拿WebSocketSession
     *
     * @param carName
     * @return
     */
    public static Channel getSessionByUserName(String carName) {
        return carNameWebsession.get(carName);
    }

    /**
     * 移除失效的WebSocketSession
     *
     * @param webSocketSession
     */
    public static void removeWebSocketSession(String carName, Channel webSocketSession) {
        if (webSocketSession == null) {
            return;
        }
        Channel webSessoin = carNameWebsession.get(carName);
        if (webSessoin == null ) {
            return;
        }
        carNameWebsession.remove(carName);
    }

    public static Set<String> getUserList() {
        return carNameWebsession.keySet();
    }
}
