package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: parking-management
 * @description: 支付结果通知
 * @author: cwf
 * @create: 2020-07-06 14:19
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {
    private String service;
    private String parkid;
    private String order_id;
    private String parking_serial;
    private String trade_no;
    private String car_number;
    private String gateid;
    private int pay_scene;
    private String pay_value;
    private String pay_time;
    private int pay_channel;

}
