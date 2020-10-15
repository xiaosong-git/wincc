package com.xiaosong.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.core.Controller;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.xiaosong.parkmodel.CheckKeyDto;
import com.xiaosong.netty.nio.WebSocketCar;
import com.xiaosong.netty.nio.service.GatewayService;
import com.xiaosong.parkmodel.DiscountResult;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import okhttp3.*;
import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class PMSWebSocketClient extends Controller implements InitializingBean {

    private static final Logger logger = Logger.getLogger(PMSWebSocketClient.class);

    public static WebSocketClient client;

    private Prop use = PropKit.use("pms.properties");

    public PMSWebSocketClient() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        createClient();
    }

    public void pms() {
//        String json = "{\"car_number\":\"皖233\",\"parkid\":\"20200628\",\"pay_scene\":0,\"service\":\"query_price\"}";
        String json = getPara("json");

        SocketChannel socketChannel = GatewayService.getChannels().get(use.get("parkId"));
        try {
            socketChannel.writeAndFlush(Unpooled.copiedBuffer(json.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        renderNull();
    }

    //创建客户端 和服务器webs 连接
    public void createClient() {

        try {
            String url = use.get("webSocketUrl");
            String parkId = use.get("parkId");
            String pmsUrl = use.get("pmsUrl");
            client = new WebSocketClient(new URI(url), new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    logger.info("连接成功");
                    //可以向webSocket server发送消息
                    CheckKeyDto checkKeyDto = new CheckKeyDto("20200628", 2, "test", "login");
                    String json = JSON.toJSONString(checkKeyDto);
                    System.out.println(json);
                    client.send(json);

                }

                //接收消息
                @Override
                public void onMessage(String msg) {

                    System.out.println("接收消息：" + msg);

                    //判断是否是json字符串
                    if (isjson(msg)) {

                        JSONObject jsonObject = JSONObject.parseObject(msg);
                        String url = (String) jsonObject.get("url");
                        Integer type = jsonObject.getInteger("type"); //1.海康pms 2. pms
                        String requestMethod = jsonObject.getString("service"); //1.海康pms 2. pms

                        if (type == 1) {
                            //海康pms
                            if (url.equals("getParkingInfo")) {
                                //获取停车场信息
                                client.send(Get(pmsUrl, url));
                            } else if (url.equals("getRoadWayPage")) {
                                //获取车道信息
                                client.send(Get(pmsUrl, url));
                            } else if (url.equals("operateBarrier")) {
                                //操作车道道闸
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("addInnerVehicle")) {
                                //白名单车辆新增或修改
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("delInnerVehicle")) {
                                //删除白名单车辆
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("addInnerVehicle")) {
                                //黑名单车辆新增或修改
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("delAlarmVehicle")) {
                                //删除黑名单车辆
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("addInnerVehicle")) {
                                //分页获取车卡资料信息
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("getVehicleRechargePage")) {
                                //分页获取固定车固定卡充值续费
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("getChargeInfoPage")) {
                                //2.1.10分页获取过车收费信息
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("getOperateRecordPage")) {
                                //分页获取操作记录信息
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("getVehicleInOutRecordPage")) {
                                //2.2.1分页获取过车记录
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("getPassVehicle")) {
                                //按时间轮询获取获取过车记录
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("ImageServer")) {
                                //获取过车图片
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("calcVehicleAccessCharge")) {
                                //实时算费
                                client.send(Post(pmsUrl, msg, url));
                            } else if (url.equals("addThirdPartyBill")) {
                                //插自助缴费和预缴费账单
                                client.send(Post(pmsUrl, msg, url));
                            }
                        } else if (type == 2) {
                            if ("query_price".equals(requestMethod)) {
                                //查询订单价格
                                queryPrice(jsonObject, requestMethod);
                            } else if ("payment_result".equals(requestMethod)) {
                                //支付结果通知
                                payResult(jsonObject, requestMethod);
                            } else {
                                //todo 做处理完下发pms
//                                SocketChannel socketChannel = GatewayService.getChannels().get(parkId);
//                                try {
//                                    socketChannel.writeAndFlush(Unpooled.copiedBuffer(msg.getBytes("UTF-8")));
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
                            }
                        }
                    }

                }

                @Override
                public void onClose(int i, String s, boolean b) {

                    logger.error("连接已关闭,尝试重连..");
                    if (client != null) {
                        client.close();
                        client = null;
                    }

                    try {
                        Thread.sleep(3000);
                        createClient();//谨慎加这句  无限循环的可能
                    } catch (InterruptedException e) {
                        logger.error("尝试重连..");
                    }
                }

                @Override
                public void onError(Exception e) {
                    //e.printStackTrace();
                    logger.error("发生错误已关闭..");
                    if (client != null) {
                        client.close();
                        client = null;
                    }

                    onClose(1, "", true);
                }
            };
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        client.connect();
//        while (!client.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
//            createClient();
//            System.out.println("重连中...");
//        }
    }

    /**
     * GET 请求
     *
     * @param PMSUrl
     * @param url
     * @return
     */
    public String Get(String PMSUrl, String url) {
        Response response = null;
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(PMSUrl + url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .build();

            response = client.newCall(request).execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.body().toString();
    }


    /**
     * @param pmsUrl 请求地址
     * @param msg    json
     * @param url    地址
     * @return
     */
    public String Post(String pmsUrl, String msg, String url)  {
        String json = "" ;
        try {
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, msg);
            Request request = new Request.Builder()
                    .url(pmsUrl + url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            json= response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void acc(){
        String json = HttpKit.readData(getRequest());
        String pmsUrl = use.get("pmsUrl");

        String body = Post(pmsUrl, json, "");
        renderJson(body);

    }

    /**
     * 查询订单价格
     *
     * @param jsonObject
     * @param requestMethod
     */
    public void queryPrice(JSONObject jsonObject, String requestMethod) {
        String parkId = jsonObject.getString("parkid");     //车场id
        String carNumber = jsonObject.getString("car_number"); //車牌
        Integer resultCode = jsonObject.getInteger("pay_scene");//支付场景


        if (parkId == null || "".equals(parkId)) {
            logger.info("缺少车场号！");
        }
        if (carNumber == null || "".equals(carNumber)) {
            logger.info("缺少车牌号！");
        }

//        String json = "{\"service\": \""+requestMethod+"\",\"parkid\": \""+parkId+"\",\"car_number\": \""+carNumber+"\",\n\"pay_scene\": "+resultCode+"\"}";
//        System.out.println(json);
        if (resultCode == 0) {
            SocketChannel socketChannel = GatewayService.getChannels().get(use.get("parkId"));
            try {
                socketChannel.writeAndFlush(Unpooled.copiedBuffer(jsonObject.toString().getBytes("UTF-8")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 支付结果通知
     *
     * @param jsonObject
     * @param requestMethod
     */
    private void payResult(JSONObject jsonObject, String requestMethod) {

        SocketChannel socketChannel = GatewayService.getChannels().get(use.get("parkId"));
        try {
            socketChannel.writeAndFlush(Unpooled.copiedBuffer(jsonObject.toString().getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 优惠券下发
     *
     * @param jsonObject
     */
    private void discountTicket(JSONObject jsonObject) {
        logger.info(jsonObject.toJSONString());
        String ticketId = jsonObject.getString("ticket_id");
        Integer type = jsonObject.getInteger("type");

        DiscountResult discountResult = JSONObject.toJavaObject(jsonObject, DiscountResult.class);

        //下发给用户
        //type=1为扫一扫发券
        if (type == 1) {
            //返回用户websocket 扫码成功
            Channel cn = WebSocketCar.getSessionByUserName(ticketId);
            cn.writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
        }
        if (jsonObject.getString("result_code").equals("0")) {
            //如果type为1 则通知商户更新二维码
            //获取商户频道
            String channel = jsonObject.getString("channel");
            //String channel = RedisUtil.getStrVal(ticketId, 5);
            logger.info("准备下发频道：" + channel);
            // 告诉商户频道需要更新二维码
            if (channel != null) {
                Channel sessionByUserName = WebSocketCar.getSessionByUserName(channel);
                logger.info("准备下发频道名：" + sessionByUserName);
                if (sessionByUserName != null) {
                    if (type == 1) {
                        discountResult.setMessage("用户扫码成功！");
                        sessionByUserName.writeAndFlush(new TextWebSocketFrame(JsonKit.toJson(discountResult)));
                    } else {

                        sessionByUserName.writeAndFlush(new TextWebSocketFrame(JsonKit.toJson(discountResult)));

                    }
                }
            }
            WebSocketCar.removeWebSocketSession(ticketId, null);

        }

    }

    /**
     * 判断是否是json字符串
     *
     * @param string 字符串
     * @return
     */
    private boolean isjson(String string) {
        try {
            JSONObject jsonStr = JSONObject.parseObject(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

