package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: parking-management
 * @description: 返回价格信息
 * @author: cwf
 * @create: 2020-06-30 17:00
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespondPrice {
    /**
     * 接口名称	query_price
     */
    private String service;
    /**
     * 返回代码	0，1，2，3, 0成功,其他失败
     */
    private int result_code;
    /**
     * 返回描述0获取成功
     */
    private String message;
    /**
     * 入场记录号	118881（对应入场时上传的order_id，每次请求相同）
     */
    private String order_id;
    /**
     * 订单流水号	10067/10068（每次请求会返回不同）
     */
    private String parking_serial;
    /**
     * 车牌号粤B99999
     */
    private String car_number;
    /**
     * 进场时间	2018-07-25 19:35:40
     */
    private String in_time;
    /**
     * 停车时长(分钟)	110
     */
    private int duration;
    /**
     * 应收金额	15.50
     */
    private String price;
    /**
     * 免费离场时间（分钟）	15
     */
    private int free_out_time;
    /**
     * 出场通道pay_scene=1或2 时返回
     */
    private String gateid;

}
