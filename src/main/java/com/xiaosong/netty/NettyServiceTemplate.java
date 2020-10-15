package com.xiaosong.netty;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public abstract class NettyServiceTemplate {
    static private EventLoopGroup bossGroup = new NioEventLoopGroup();
    static private EventLoopGroup workerGroup = new NioEventLoopGroup();

    abstract protected ChannelHandler[] createHandlers();

    abstract public int getPort();

    abstract public String getName();

    @PostConstruct
    public void start() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelHandler[] handlers = createHandlers();
                        for (ChannelHandler handler : handlers) {
                            ch.pipeline().addLast(handler);
                        }
                    }
                }).option(ChannelOption.SO_BACKLOG, 128).option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_REUSEADDR, true);

        ChannelFuture cf = b.bind(getPort()).await();
        // cf.channel().closeFuture().await();
        if (!cf.isSuccess()) {
            System.out.println("无法绑定端口：" + getPort());
            throw new Exception("无法绑定端口：" + getPort());
        }

        System.out.println("服务[{" + getName() + "}]启动完毕，监听端口[{" + getPort() + "}]");
    }

    @PreDestroy
    public void stop() {
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
        System.out.println("服务[{" + getName() + "}]关闭。");
    }
}
