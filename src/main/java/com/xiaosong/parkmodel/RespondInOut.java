package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @program: parking-management
 * @description:车辆进出日志
 * @author: cwf
 * @create: 2020-07-07 17:58
 **/
@Data
@AllArgsConstructor
public class RespondInOut {
    private String service;
    private int result_code;
    private String message;
    private String order_id;


}
