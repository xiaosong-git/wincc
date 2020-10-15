package com.xiaosong.common.wincc.device;

import com.dhnetsdk.lib.NetSDKLib;
import com.xiaosong.model.TbDevice;

public class TbDeviceService {

    public static final	TbDeviceService me = new TbDeviceService();

    static final TbDevice dao = TbDevice.dao;

    public TbDevice findByDeviceIp(String deviceIp){
        return  dao.findFirst("select * from tb_device where deviceIp = ?",deviceIp);
    }

    public TbDevice findDeviceIp(NetSDKLib.LLong m_hAttachHandle) {
        return  dao.findFirst("select * from tb_device where deviceName = ?",String.valueOf(m_hAttachHandle));

    }
}
