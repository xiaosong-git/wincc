package com.xiaosong.util;

public class NameUtils {

    //姓名隐藏
    public static String AccordingToName(String username) {
        StringBuffer name = null;
        if (username.length() >= 2 && username.length() <= 3) {
            name = new StringBuffer(username);
            //创建StringBuffer对象strb
            name.setCharAt(1, '*');    //修改指定位置的字符
            //输出strb 的长度
            //name.setLength(6);      //设置字符串长度，超出部分会被裁剪
            return new String(name);
        }else if (username.length() >= 4) {
            name = new StringBuffer(username);
            //创建StringBuffer对象strb
            name.setCharAt(1, '*');    //修改指定位置的字符
            name.setCharAt(2, '*');    //修改指定位置的字符
            //输出strb 的长度
            //name.setLength(6);
            return new String(name);
        }
        return username;
    }
}
