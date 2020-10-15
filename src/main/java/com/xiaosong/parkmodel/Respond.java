package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @program: testnetty
 * @description: 认证
 * @author: cwf
 * @create: 2020-06-28 11:48
 **/
@Data
@AllArgsConstructor
public class Respond {
    private String service;
    private int result_code;
    private String message;
    //构造私有化，防止直接new
    private Respond(){}

    //静态初始化器（static initializer）中创建实例，保证线程安全
    private static Respond instance = new Respond();

    public static Respond getInstance(){
        return instance;
    }
    public  Respond checkOk(){
        instance.setService("checkKey");
        instance.setResult_code(0);
        instance.setMessage("认证成功");
        return instance;
    }
    public Respond checkFail(){
        instance.setService("checkKey");
        instance.setResult_code(1);
        instance.setMessage("认证失败，无此车场");
        return instance;
    }
    public Respond heartBeatOk(){
        instance.setService("heartbeat");
        instance.setResult_code(0);
        instance.setMessage("在线");
        return instance;
    }
    public Respond heartBeatFail(){
        instance.setService("heartbeat");
        instance.setResult_code(1);
        instance.setMessage("离线");
        return instance;
    }
}
