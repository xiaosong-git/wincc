package com.xiaosong.netty.nio.handler;

import com.xiaosong.netty.nio.service.StringTCPService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @program: testnetty
 * @description:
 * @author: cwf
 * @create: 2020-06-23 15:04
 **/
public class StringTCPServerHandler extends SimpleChannelInboundHandler<String> {


    private StringTCPService ns=new StringTCPService();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String message) throws Exception {
        //由于用"}"进行拆包所以 message后需要加回来"}"
        String handle = ns.handle(channelHandlerContext,message+"}");
        if (!"".equals(handle)) {
            channelHandlerContext.channel().writeAndFlush(Unpooled.copiedBuffer(handle.getBytes("UTF-8")));
        }
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("有新客户端连接:"+ctx.channel().remoteAddress());

    }
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        ctx.write(msg); // (1)
//        ctx.flush(); // (2)
//    }
}
