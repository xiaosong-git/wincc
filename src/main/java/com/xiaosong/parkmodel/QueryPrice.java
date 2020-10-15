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
public class QueryPrice {
    private String service;
    private String parkid;
    private int pay_scene;
    private String order_id;
    private String car_number;
    private String gateid;
}
