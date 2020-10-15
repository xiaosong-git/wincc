package com.xiaosong.config.quartz;

import com.xiaosong.netty.PMSWebSocketClient;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.nio.channels.NotYetConnectedException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  发送心跳
 */
public class sendHeartbeat implements Job {
    private static Logger logger = Logger.getLogger(sendHeartbeat.class);
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            String json = "{\"service\": \"SWJHeartbeat\",\"time\": \""+getDateTime()+"\"}";
            if(PMSWebSocketClient.client!=null){
                //System.out.println("发送心跳:"+json);
                PMSWebSocketClient.client.send(json);

            }else{
                logger.error("WebSocket断开...");
            }
        } catch (NotYetConnectedException e) {
            logger.error("WebSocket断开...");
        }
    }

    //获取当前时间的  年-月-日  时:分:秒
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }
}
