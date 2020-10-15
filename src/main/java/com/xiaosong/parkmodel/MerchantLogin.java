package com.xiaosong.parkmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: parking-management
 * @description:
 * @author: cwf
 * @create: 2020-07-21 17:02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantLogin {
    private String service;
    private int result_code;
    private String message;
    private String parkinUrl;
}
