package com.xiaosong.common.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaosong.common.accessrecord.AccessRecordService;
import com.xiaosong.model.TbAccessrecord;
import com.xiaosong.util.NameUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 〈webSocket功能〉
 *
 * @author foam103
 * @create 2020/3/15
 */
@ServerEndpoint("/websocket.ws/{userId}")
public class WebSocket {

    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();
    private Session session;
    private String userId;

    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session) throws IOException {

        this.userId = userId;
        this.session = session;
        clients.put(userId, this);
        //System.out.println("clients:"+clients);
        System.out.println("已连接");
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            System.out.println("收到消息:"+message);
            if(message.equals("init")){
                List<TbAccessrecord> accessrecords = TbAccessrecord.dao.find("select * from tb_accessrecord order BY scanDate desc,scanTime desc LIMIT 10 ");
                JSONArray json = new JSONArray();

                for(TbAccessrecord accessrecord : accessrecords){
                    JSONObject jo = new JSONObject();
                    jo.put("scanDate", accessrecord.getScanDate()+" "+accessrecord.getScanTime());
                    jo.put("tmp", accessrecord.getTemperature());
                    jo.put("deviceIp", accessrecord.getDeviceIp());
                    jo.put("userName", NameUtils.AccordingToName(accessrecord.getUserName()));
                    jo.put("photo", accessrecord.getPhoto());
                    jo.put("userType", accessrecord.getUserType());
                    jo.put("deviceType", accessrecord.getDeviceType());

                    json.add(jo);
                }

                AccessRecordService srv = AccessRecordService.me;
                String accDate = getDate();
                //人脸设备
                Integer face = srv.findFace(accDate);
                //二维码设备
                Integer qrCode = srv.findQRCode(accDate);
                //员工通行
                Integer staff = srv.findStaff(accDate);
                //访客通行
                Integer visitor = srv.findVisitor(accDate);
                JSONObject jo = new JSONObject();
                jo.put("face",face);
                jo.put("qrCode",qrCode);
                jo.put("staff",staff);
                jo.put("visitor",visitor);
                json.add(jo);

                // 判断是否在线，如果在线发送信息
                for (WebSocket item : clients.values()) {
    //            item.session.getBasicRemote().sendText(accessrecords.toString());
                    item.session.getBasicRemote().sendText(json.toString());
                }
            }else{
                for (WebSocket item : clients.values()) {
    //            item.session.getBasicRemote().sendText(accessrecords.toString());
                    //item.session.getBasicRemote().sendText(message);
                    System.out.println(item.session.getOpenSessions().hashCode());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // message是json格式
//        JSONObject obj = JSONObject.parseObject(message);
//        String user = obj.get("userId").toString();
//        String mes = obj.get("message").toString();
//        // 判断是否在线，如果在线发送信息
//        for (WebSocket item : clients.values()) {
//            if (item.userId.equals(user)) {
//                System.out.println(mes);
//                item.session.getAsyncRemote().sendText(mes);
//            }
//        }
    }


    //获取 当前时间的 年-月-日
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

}
