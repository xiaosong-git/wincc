package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: parking-management
 * @description: 下发卡券结果
 * @author: cwf
 * @create: 2020-07-20 10:45
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountResult {
    private String service;
    private int result_code;
    private String message;
    private String order_id;
    private String ticket_id;

}
