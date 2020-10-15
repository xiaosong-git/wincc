package com.xiaosong.netty.nio.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.xiaosong.parkmodel.QueryPriceDto;
import com.xiaosong.netty.PMSWebSocketClient;
import com.xiaosong.parkmodel.Respond;
import com.xiaosong.parkmodel.RespondInOut;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @program: parking-management
 * @description: netty接口方法
 * @author: cwf
 * @create: 2020-06-28 14:28
 **/
public class StringTCPService {

    Logger logger = Logger.getLogger(StringTCPService.class);

    private Respond respond = Respond.getInstance();

    private Prop use = PropKit.use("pms.properties");

    public String handle(ChannelHandlerContext ctx, String message) {
        String serverUrl = use.get("serverUrl");

        JSONObject jsonObject = null;
        try {
            jsonObject = JSON.parseObject(message);
        } catch (Exception e) {
            System.out.println("车牌不存在.");
            //PMSWebSocketClient.client.send("车牌不存在.");
            return "";
        }
        String methodName = jsonObject.getString("service");
        if ("checkKey".equals(methodName)) {
            logger.info("验证登入:↓\n" + jsonObject);
            return checkKey(ctx, jsonObject);
        }
//        else if ("heartbeat".equals(methodName)) {
//
//            System.out.println(message);
//        }
        else if ("uploadcarin".equals(methodName)) {
            String service = jsonObject.getString("service");
            String cardType = jsonObject.getString("card_type");
            String gateInId = jsonObject.getString("gateinid");
            String inTime = jsonObject.getString("in_time");
            String operaTorin = jsonObject.getString("operatorin");
            String parkId = jsonObject.getString("parkid");
            String orderId = jsonObject.getString("order_id");
            String carNumber = jsonObject.getString("car_number");
            String gateInName = jsonObject.getString("gateinname");
            String carType = jsonObject.getString("car_type");

            //获取 记录
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");

            RequestBody body = RequestBody.create(mediaType,
                    "{\n" +
                            "\t\"breaksAmount\": \"\",\n" +
                            "\t\"carNumber\": \"" + carNumber + "\",\n" +
                            "\t\"carType\": " + carType + ",\n" +
                            "\t\"cardType\": " + cardType + ",\n" +
                            "\t\"discountAmount\": \"\",\n" +
                            "\t\"discountNo\": \"\",\n" +
                            "\t\"discountReason\": \"\",\n" +
                            "\t\"gateinid\": " + gateInId + ",\n" +
                            "\t\"gateinname\": \"" + gateInName + "\",\n" +
                            "\t\"gateoutid\": \"\",\n" +
                            "\t\"gateoutname\": \"\",\n" +
                            "\t\"id\": 0,\n" +
                            "\t\"inTime\": \"" + inTime + "\",\n" +
                            "\t\"operatorin\": \"" + operaTorin + "\",\n" +
                            "\t\"operatorout\": \"\",\n" +
                            "\t\"orderId\": \"" + orderId + "\",\n" +
                            "\t\"outTime\": \"\",\n" +
                            "\t\"parkid\": \"" + parkId + "\",\n" +
                            "\t\"payType\": \"\",\n" +
                            "\t\"paycharge\": \"\",\n" +
                            "\t\"payed\": \"\",\n" +
                            "\t\"realcharge\": \"\",\n" +
                            "\t\"remark\": \"\"\n" +
                            "}");
            Request request = new Request.Builder()
                    .url(serverUrl+"park/pms/InOutRecord")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String string = response.body().string();
                JSONObject json = JSONObject.parseObject(string);
                String verify = json.getString("verify");
                JSONObject sign = JSONObject.parseObject(verify);
                String success = sign.getString("sign");
                if("success".equals(success)){
                    System.out.println("入场记录上传成功~");
                    RespondInOut respondInOut = new RespondInOut(methodName, 0, "上传成功", orderId);
                    String js = JSON.toJSONString(respondInOut);
                    SocketChannel socketChannel = GatewayService.getChannels().get(use.get("parkId"));
                    try {
                        socketChannel.writeAndFlush(Unpooled.copiedBuffer(js.getBytes("UTF-8")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if ("uploadcarout".equals(methodName)){

            String service = jsonObject.getString("service");
            String cardType = jsonObject.getString("card_type");
            String gateInId = jsonObject.getString("gateinid");
            String inTime = jsonObject.getString("in_time");
            String operaTorin = jsonObject.getString("operatorin");
            String parkId = jsonObject.getString("parkid");
            String orderId = jsonObject.getString("order_id");
            String carNumber = jsonObject.getString("car_number");
            String gateInName = jsonObject.getString("gateinname");
            String carType = jsonObject.getString("car_type");
            //出场
            String outTime = jsonObject.getString("out_time");
            String gateOutId = jsonObject.getString("gateoutid");
            String gateOutName = jsonObject.getString("gateoutname");
            String operaTorOut = jsonObject.getString("operatorout");
            String payCharge = jsonObject.getString("paycharge");
            String realCharge = jsonObject.getString("realcharge");
            String breaksAmount = jsonObject.getString("breaks_amount");
            String discountAmount = jsonObject.getString("discount_amount");
            String discountNo = jsonObject.getString("discount_no");
            String discountReason = jsonObject.getString("discount_reason");
            String payType = jsonObject.getString("pay_type");
            String payed = jsonObject.getString("payed");
            String remark = jsonObject.getString("remark");

            //获取 记录
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType,
                    "{\n" +
                            "\t\"breaksAmount\": \""+breaksAmount+"\",\n" +
                            "\t\"carNumber\": \""+carNumber+"\",\n" +
                            "\t\"carType\": "+carType+",\n" +
                            "\t\"cardType\": "+cardType+",\n" +
                            "\t\"discountAmount\": \""+discountAmount+"\",\n" +
                            "\t\"discountNo\": \""+discountNo+"\",\n" +
                            "\t\"discountReason\": \""+discountReason+"\",\n" +
                            "\t\"gateinid\": "+gateInId+",\n" +
                            "\t\"gateinname\": \""+gateInName+"\",\n" +
                            "\t\"gateoutid\": "+gateOutId+",\n" +
                            "\t\"gateoutname\": \""+gateOutName+"\",\n" +
                            "\t\"id\": 0,\n" +
                            "\t\"inTime\": \""+inTime+"\",\n" +
                            "\t\"operatorin\": \""+operaTorin+"\",\n" +
                            "\t\"operatorout\": \""+operaTorOut+"\",\n" +
                            "\t\"orderId\": \""+orderId+"\",\n" +
                            "\t\"outTime\": \""+outTime+"\",\n" +
                            "\t\"parkid\": \""+parkId+"\",\n" +
                            "\t\"payType\": \""+payType+"\",\n" +
                            "\t\"paycharge\": \""+payCharge+"\",\n" +
                            "\t\"payed\": "+payed+",\n" +
                            "\t\"realcharge\": \""+realCharge+"\",\n" +
                            "\t\"remark\": \""+remark+"\"\n" +
                            "}");
            Request request = new Request.Builder()
                    .url(serverUrl+"park/pms/InOutRecord")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String string = response.body().string();
                JSONObject json = JSONObject.parseObject(string);
                String verify = json.getString("verify");
                JSONObject sign = JSONObject.parseObject(verify);
                String success = sign.getString("sign");
                if("success".equals(success)){
                    System.out.println("出场记录上传成功~");
                    RespondInOut respondInOut = new RespondInOut(methodName, 0, "上传成功", orderId);
                    String js = JSON.toJSONString(respondInOut);
                    SocketChannel socketChannel = GatewayService.getChannels().get(use.get("parkId"));
                    try {
                        socketChannel.writeAndFlush(Unpooled.copiedBuffer(js.getBytes("UTF-8")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if("query_price".equals(methodName)){
            System.out.println("订单查询成功..");
            String parkId = use.get("serverUrl");

            String service = jsonObject.getString("service");
            String result_code = jsonObject.getString("result_code");
            String messAge = jsonObject.getString("message");
            String order_id = jsonObject.getString("order_id");
            String parking_serial = jsonObject.getString("parking_serial");
            String car_number = jsonObject.getString("car_number");
            String in_time = jsonObject.getString("in_time");
            String duration = jsonObject.getString("duration");
            String price = jsonObject.getString("price");
            String free_out_time = jsonObject.getString("free_out_time");
            String gateid = jsonObject.getString("gateid");

            QueryPriceDto queryPriceDto = new QueryPriceDto(parkId, service, result_code, messAge, order_id, parking_serial, car_number, in_time, duration, price, free_out_time, gateid);
            String json = JSON.toJSONString(queryPriceDto);

            PMSWebSocketClient.client.send(json);
        }else if("payment_result".equals(methodName)){
            System.out.println("支付结果通知成功..");
            PMSWebSocketClient.client.send(message);
        }

        return "";
    }

    public static String camelName(String name) {
        StringBuilder result = new StringBuilder();
        if ((name == null) || (name.isEmpty())) {
            return "";
        }
        if (!name.contains("_")) {
            return name.toLowerCase();
        }
        String[] camels = name.split("_");
        for (String camel : camels) {
            if (!camel.isEmpty()) {
                if (result.length() == 0) {
                    result.append(camel.toLowerCase());
                } else {
                    result.append(camel.substring(0,1).toUpperCase());
                    result.append(camel.substring(1).toLowerCase());
                }
            }
        }
        System.err.println("camelName:" + result.toString());
        return result.toString();
    }

//    private void discountTicket(JSONObject jsonObject, String methodName) {
//        logger.info(jsonObject.toJSONString());
//        String ticketId = jsonObject.getString("ticket_id");
//
//        DiscountResult discountResult = JSONObject.toJavaObject(jsonObject, DiscountResult.class);
//        TTicketCompany first = TTicketCompany.dao.findFirst("select * from t_ticket_company where ticket_id=?", ticketId);
//        first.setStatus("2").update();
//        Integer type = first.getType();
//        //下发给用户
//        //type=1为扫一扫发券
//        if (type==1) {
//            //返回用户websocket 扫码成功
//            Channel cn = WebSocketCar.getSessionByUserName(ticketId);
//            cn.writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
//        }
//        if (jsonObject.getString("result_code").equals("0")) {
//            //如果type为1 则通知商户更新二维码
//            //获取商户频道
//            String channel = RedisUtil.getStrVal(ticketId, 5);
//            logger.info("准备下发频道："+channel);
//            // 告诉商户频道需要更新二维码
//            if (channel != null) {
//                Channel sessionByUserName = WebSocketCar.getSessionByUserName(channel);
//                logger.info("准备下发频道名："+sessionByUserName);
//                if (sessionByUserName != null) {
//                    if (type == 1) {
////                        discountResult.setMessage("用户扫码成功！");
////                        sessionByUserName.writeAndFlush(new TextWebSocketFrame(JsonKit.toJson(discountResult)));
//                    } else {
//
//                        sessionByUserName.writeAndFlush(new TextWebSocketFrame(JsonKit.toJson(discountResult)));
//
//                    }
//                }
//            }
//            RedisUtil.del(4, ticketId);
//            RedisUtil.del(5, ticketId);
//
//            WebSocketCar.removeWebSocketSession(ticketId, null);
//
//        }
//
//    }
//
//

    /**
     * 验证车场与key
     */
    public String checkKey(ChannelHandlerContext ctx, JSONObject jsonObject) {

//        Record first = Db.findFirst("select * from t_park where parkid=? and parkkey=?", jsonObject.get("parkid"), jsonObject.get("parkkey"));
        GatewayService.addGatewayChannel(jsonObject.getString("parkid"), (SocketChannel) ctx.channel());
        return JSON.toJSONString(respond.checkOk());
    }
//
//    /**
//     * 返回心跳
//     */
//    public String heartbeat(JSONObject jsonObject) {
//
//        return JSON.toJSONString(respond.heartBeatOk());
//    }
//
//    /**
//     * 下发给websocket
//     *
//     * @param jsonObject
//     */
//    public void queryPrice(JSONObject jsonObject) {
//        String parkid = jsonObject.getString("parkid");
//        String car_number = jsonObject.getString("car_number");
//        String result_code = jsonObject.getString("result_code");
//        if ("0".equals(result_code)) {
//            //存储停车管理订单信息到数据库
//            TParkOrder tParkOrder = JSONObject.toJavaObject(jsonObject, TParkOrder.class);
//            //流水号作订单号
//            tParkOrder.setParkOrderId(tParkOrder.getParkingSerial());
//            String parkOrderId = tParkOrder.getParkOrderId();
//            String price = tParkOrder.getPrice();
////            TParkOrder first = TParkOrder.dao.findFirst("select id from t_park_order where park_order_id=?", parkOrderId);
//            boolean suc;
//            suc = tParkOrder.save();
//
//            try {
//
//                BigDecimal moneyDecimal = new BigDecimal(price);
//                String orderAmt = moneyDecimal.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();
//                System.out.println("乘100转换后的金额" + orderAmt);
//                if (!"0".equals(orderAmt)) {
//                    //todo 上生产后要把amt改为price
//                    String allinUrl = AllinPayService.me.getUrl(parkOrderId, "1", car_number, parkid);
//                    // 生成通联支付url
//                    jsonObject.put("allin_url", allinUrl);
//                }
//
//            } catch (Exception e) {
//                logger.error("生成通联支付url失败", e);
//            }
//        }
//        //发送给webscoket连接者
//        Channel cn = WebSocketCar.getSessionByUserName(parkid + "_" + car_number);
//        cn.writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
//    }
//
//    /**
//     * 返回保存结果
//     *
//     * @param jsonObject
//     */
//    public void paymentResult(JSONObject jsonObject) {
//
//    }
//
//    /**
//     * f
//     * 车辆入场出场
//     *
//     * @param jsonObject
//     */
//    private String uploadCarInOut(JSONObject jsonObject, String methodName) {
//        TParkInout tParkInout = JSONObject.toJavaObject(jsonObject, TParkInout.class);
//        Long id = Db.queryLong("select id from t_park_inout where parkid=? and order_id=? limit 1",tParkInout.getParkid(),tParkInout.getOrderId());
//        boolean update =false;
//        if (id!=null){
//            update= tParkInout.setId(id).update();
//        }else {
//            update = tParkInout.save();
//        }
//        if (update) {
//            RespondInOut respondInOut = new RespondInOut(methodName, 0, "上传成功", jsonObject.getString("order_id"));
//            return JSON.toJSONString(respondInOut);
//        }
//        //todo 存储数据
//        return "";
//
//    }

}
