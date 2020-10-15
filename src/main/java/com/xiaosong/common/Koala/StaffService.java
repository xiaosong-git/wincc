package com.xiaosong.common.Koala;

import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;

import java.util.List;

public class StaffService {
    public static final StaffService me = new StaffService();

    public void delete(String sid) {
        Db.delete("delete from tb_companyuser where userId = ?",sid);
    }

    /**
     * 根据id查询  用户
     * @param id
     */
    public TbCompanyuser findUserId(String id) {
        return TbCompanyuser.dao.findFirst("select * from tb_companyuser where userId = ?", id);
    }

    /**
     *  查询所有 的 设备ip
     */
    public List<TbDevice> findAllDevice() {
        return TbDevice.dao.find("select * from tb_device");
    }
}
