package com.xiaosong.netty.nio.handler;

import com.alibaba.fastjson.JSON;
import com.xiaosong.parkmodel.Respond;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cwf
 */
public class HeartBeatServerHandler extends ChannelInboundHandlerAdapter {

    Logger logger = Logger.getLogger(HeartBeatServerHandler.class);
    // Return a unreleasable view on the given ByteBuf
    // which will just ignore release and retain calls.
    private AtomicInteger count = new AtomicInteger(1);
    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
            .unreleasableBuffer(Unpooled.copiedBuffer(JSON.toJSONString(new Respond("heartbeat",1,"离线")),
                    CharsetUtil.UTF_8));  // 1

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {

        if (evt instanceof IdleStateEvent) {  // 2
            IdleStateEvent event = (IdleStateEvent) evt;
            String type = "";
            SocketAddress socketAddress = ctx.channel().remoteAddress();
            if (event.state() == IdleState.READER_IDLE) {
                count.incrementAndGet();
                if (count.get() > 3) {
                    logger.info(socketAddress+"客户端还在？？ 已经3次检测没有访问了，我要断开了哦！！！");
                    ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate()).addListener(
                            ChannelFutureListener.CLOSE_ON_FAILURE);  // 3
                    ctx.channel().close();

                }
                type = "read idle";
            } else if (event.state() == IdleState.WRITER_IDLE) {
                type = "write idle";
            } else if (event.state() == IdleState.ALL_IDLE) {
                type = "all idle";
            }
            logger.info( socketAddress +"超时类型：" + type);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}