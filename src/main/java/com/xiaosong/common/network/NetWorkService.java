package com.xiaosong.common.network;

import com.xiaosong.model.TbNetwork;

public class NetWorkService {
    public static final NetWorkService me = new NetWorkService();

    /**
     * 查询 上位机 的 IP地址
     * @return
     */
    public TbNetwork findNetWork() {
        return TbNetwork.dao.findFirst("select * from tb_network");
    }
}
