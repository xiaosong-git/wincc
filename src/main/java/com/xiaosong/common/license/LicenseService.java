package com.xiaosong.common.license;

import com.xiaosong.model.TbLicense;

public class LicenseService {
    public static LicenseService me = new LicenseService();

    /**
     * 根据mac 地址 查询 license 数据
     * @param mac
     * @return
     */
    public TbLicense findMac(String mac) {
        return TbLicense.dao.findFirst("select * from tb_license where mac = ?",mac);
    }

    /**
     * 查询license 数据
     */
    public TbLicense findLicense() {
        return TbLicense.dao.findFirst("select * from tb_license");
    }
}
