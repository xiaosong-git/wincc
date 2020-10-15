package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
public class CheckKeyDto implements Serializable {
    private static final long serialVersionUID = -130770253863883634L;
    //"通道名"
    private String channel;
    //"类型 1.上位机 2.停车场 3.商户 4.用户 5.车牌号"
    private Integer type;
    //"权限验证"
    private String sign;
    //"请求"
    private String service;
}
