package com.xiaosong.netty.nio;

import com.xiaosong.netty.NettyServiceTemplate;
import com.xiaosong.netty.nio.handler.HeartBeatServerHandler;
import com.xiaosong.netty.nio.handler.StringTCPServerHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


public class StringTcpServer extends NettyServiceTemplate {
    private int port =8077 ;
    private String name = "TCP Server";

    public StringTcpServer(int port) {
        this.port = port;
    }

    @Override
    protected ChannelHandler[] createHandlers() {
        return new ChannelHandler[] {
                //分词器，解决粘包拆包问题
                new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer("}".getBytes())),
                new StringDecoder(),
                new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS),
                //心跳连接处理超时连接
                new HeartBeatServerHandler(),
                //自定义tcp业务处理器
                new StringTCPServerHandler() };
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setName(String name) {
        this.name = name;
    }

}