package com.xiaosong.util;


import cn.hutool.crypto.SmUtil;

/**
 * @author zengxq
 * @create 2020-05-23 21:08
 * @Description 签名校验类
 */
public class SignUtil {

    public static boolean check(String sn,String time,String body,String sign){
        String compareSign=check(sn,time,body);
        if (sign.equalsIgnoreCase(compareSign)){
            return true;
        }
        return false;
    }

    public static boolean check(String sn,String time,String body,String secret,String sign){
        String compareSign=orgCheck(sn,time,body,secret);
        if (sign.equalsIgnoreCase(compareSign)){
            return true;
        }
        return false;
    }

    public static String  check(String sn,String time,String body){
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append(sn);
        stringBuffer.append(time);
        stringBuffer.append(body);
        String compareSign= GmSmUtil.sm3(stringBuffer.toString());
        return compareSign;
    }

    public static String orgCheck(String orgId,String time,String body,String secret){
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append(orgId);
        stringBuffer.append(time);
        stringBuffer.append(body);
        stringBuffer.append(secret);
        String compareSign=GmSmUtil.sm3(stringBuffer.toString());
        return compareSign;
    }


    public static void main(String[] args) {
        String body="{\"bizPackage\":{\"idType\":\"411\",\"name\":\"林霄\",\"number\":\"xxxx\",\"reportTime\":\"1590718194009\"},\"bizType\":\"04\",\"requestId\":\"1c084182e5924d07801baa608fff9a96\",\"txPackage\":{\"mobile\":\"13850151069\",\"validResult\":\"1\",\"validTime\":\"1590718194008\"},\"txType\":\"100010\"}";
        //
        String sm4=SmUtil.sm4("xxxxxxx".getBytes()).encryptBase64(body);

        String orgId="xxxxxx";
        System.out.println(sm4);
        String checkStr=orgCheck("orgid",System.currentTimeMillis()+"",body,sm4);
        System.out.println(checkStr);
    }


}
