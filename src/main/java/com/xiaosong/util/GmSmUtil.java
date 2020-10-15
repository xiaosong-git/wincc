package com.xiaosong.util;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.Digester;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

/**
 * @author zengxq
 * @create 2019-11-19 13:57
 * @Description 国密算法
 */
public class GmSmUtil {

    private static String SM3 = "SM3";
    private static final byte[] SECRET_BYTES="d52952e75e8b45a7".getBytes();

    /**
     * 加密，使用UTF-8编码
     *
     * @param data 被加密的字符串
     * @return 加密后的Base64
     */
   public static String encryptBase64(String data){
       if (StringUtils.isNotBlank(data)){
           return SmUtil.sm4(SECRET_BYTES).encryptBase64(data);
       }
       return data;
    }

    /**
     * 解密Hex表示的字符串，默认UTF-8编码
     *
     * @param data 被解密的String
     * @return 解密后的String
     */
    public static String  decryptStr(String data){
        if (StringUtils.isNotBlank(data)){
            return SmUtil.sm4(SECRET_BYTES).decryptStr(data);
        }
        return data;
    }


    public static String sm3(String data){

        return Base64.getEncoder().encodeToString(new Digester(SM3).digest(data));
    }
    public static void main(String[] args) {

          String encryStr=GmSmUtil.encryptBase64("350481198204226527");
          System.out.println(encryStr);
          String decryStr=GmSmUtil.decryptStr(encryStr);
          System.out.println(decryStr);
          System.out.println(sm3("123456"));

    }


}
