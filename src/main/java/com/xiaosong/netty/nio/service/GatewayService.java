package com.xiaosong.netty.nio.service;

import io.netty.channel.socket.SocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: parking-management
 * @description:
 * @author: cwf
 * @create: 2020-06-28 16:00
 **/
public class GatewayService {

    private static Map<String, SocketChannel> map = new ConcurrentHashMap<>();

    public static void addGatewayChannel(String id, SocketChannel gateway_channel){
        map.put(id, gateway_channel);
    }

    public static Map<String, SocketChannel> getChannels(){
        return map;
    }

    public static SocketChannel getGatewayChannel(String id){
        return map.get(id);
    }

    public static void removeGatewayChannel(String id){
        map.remove(id);
    }

}