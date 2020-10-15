package com.xiaosong.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SmUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @author zengxq
 * @create 2020-07-07 21:28
 * @Description 网证组件测试类
 */
public class NewWorldAuth {

    static Logger logger = Logger.getLogger(NewWorldAuth.class);
    // 机构id
    public static final String ORG_ID = "1594625187126001";

    // 服务端数据加密公钥
    public static final String SERVER_KEY = "a899ff3819b14bb7";

    //接口地址
    public static final String SERVER_URL = "http://nlp53.nlpublic.com:30001/door/server/v1/auth/add";

    //设备序列号
    public static final String SN = "A8F470B49817";

    public static JSONObject sendPost(String number, String name, String photoData, String type,
                                      String startTime, String endTime, String bid, String rid, String sn) {
        // 请求header
        String time = String.valueOf(System.currentTimeMillis());
        // 请求body报文
        JSONObject jsonObject = new JSONObject();
        //jsonObject.put("requestId", "85addc0036864a0ba3e0498f90d4401a");
//        jsonObject.put("bid", "EceA7Y7CEAA=");
//        jsonObject.put("sn","0100000000002686");
        jsonObject.put("requestId", bid);
        jsonObject.put("rid", rid);
        jsonObject.put("sn", sn);
        jsonObject.put("name", name);
        jsonObject.put("number", number);
        //jsonObject.put("photoData", photoData);
        jsonObject.put("type", type);
        jsonObject.put("startTime", startTime);
        jsonObject.put("endTime", endTime);
        String body = jsonObject.toJSONString();
//        System.out.println("客户端加密前发送数据：" + body);
        String bodyStr = SmUtil.sm4(SERVER_KEY.getBytes()).encryptBase64(body);
//        System.out.println("客户端加密后发送数据：" + bodyStr);
        String sign = SignUtil.orgCheck(ORG_ID, time, bodyStr, SERVER_KEY);
//        System.out.println("sign：" + sign);

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body1 = RequestBody.create(mediaType, bodyStr);
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .method("POST", body1)
                .addHeader("orgid", ORG_ID)
                .addHeader("sign", sign)
                .addHeader("time", time)
                .addHeader("Content-Type", "application/json;charset=utf-8")
                .build();
        Response response = null;
        JSONObject returnObject = null;
        try {
            response = client.newCall(request).execute();
            String string = response.body().string();
            logger.info("服务端响应数据：" + string);
            // 解密响应数据
            returnObject = JSONObject.parseObject(string);
            return returnObject;

        } catch (IOException e) {
            logger.error("调用新大陆接口错误", e);
            returnObject.put("msg", "系统错误");
            returnObject.put("code", "-1");
            return returnObject;
        }

    }

    public static void main(String[] args) throws Exception {
//        String decode = DESUtil.decode(key, s);
//        sendPost("陈维发","350121199306180330",Configuration.GetImageStrFromPath("http://47.98.205.206/imgserver/" + "user/125/1593398235082.jpg", 40));
//        JSONObject jsonObject = sendPost( "350121199306180330","陈维发", Base64.encode((FilesUtils.getImageFromNetByUrl("http://47.98.205.206/imgserver/" + "user/125/1594131603531.jpg"))));
        byte[] imageFromNetByUrl = FilesUtils.getImageFromNetByUrl("http://47.98.205.206/imgserver/" + "user/817/1573474833881.jpg");
        byte[] data = FilesUtils.compressUnderSize(imageFromNetByUrl, 40960L);
        //logger.info("imageFromNetByUrl:{}"+imageFromNetByUrl);
        //logger.info("data:{}"+data);
        //logger.info("压缩前后对比{}"+Arrays.equals(imageFromNetByUrl,data));
        String number = "350823199405224918";
        String name = "林福";
        String encode = Base64.encode(data);
        String type = "1";
        String startTime = "1594366643921";
        String endTime = "1994366643921";
        String bid = "EceA7Y7CEAA1";
        String rid = "";
        JSONObject jsonObject = sendPost(number, name, "",type,startTime,endTime,bid,rid,SN);
//        JSONObject jsonObject = sendPost("350121199306180330", "陈维发", Base64.encode(data));
        if ("0".equals(jsonObject.getString("code"))) {
            String data1 = jsonObject.getString("data");
            if (StringUtils.isNotBlank(data1)) {
                data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                JSONObject value = JSON.parseObject(data1);
                System.out.println(data1);
                logger.info("data信息为{}" + value.toJSONString());
                //String bid = value.getString("bid");
                logger.info("服务端响应解密后数据：" + jsonObject);
            }
        } else {
            logger.error("失败原因：{}" + jsonObject.getString("msg"));
        }
//        ThreadTestMethod.auth(decode, "易超", Base64.encode(data));

//        ThreadTestMethod.phoneResult(decode,"易超","user/2021/1594200468327.jpg");
    }


}
