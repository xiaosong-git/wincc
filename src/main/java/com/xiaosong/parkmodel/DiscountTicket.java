package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: parking-management
 * @description: 查询票价
 * @author: cwf
 * @create: 2020-06-28 17:29
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountTicket {
    private String service;
    private String parkid;
    private String order_id;
    private String car_number;
    private String ticket_id;
    private int ticket_type;
    private String ticket_reason;
    private String money;
    private int duration;
    private String shop_name;
    private int limit_time;
}
