package com.xiaosong.parkmodel;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
public class QueryPriceDto implements Serializable {

    private static final long serialVersionUID = -130770253863883634L;
    @ApiModelProperty("parkid")
    private String parkid;
    @ApiModelProperty("service")
    private String service;
    @ApiModelProperty("结果code")
    private String resultCode;
    @ApiModelProperty("消息")
    private String message;
    @ApiModelProperty("出入场记录号")
    private String orderId;
    @ApiModelProperty("订单流水号")
    private String parkingSerial;
    @ApiModelProperty("车牌号")
    private String carNumber;
    @ApiModelProperty("入场时间")
    private String inTime;
    @ApiModelProperty("分钟")
    private String duration;
    @ApiModelProperty("price")
    private String price;
    @ApiModelProperty("freeOutTime")
    private String freeOutTime;
    @ApiModelProperty("gateid")
    private String gateid;
}
