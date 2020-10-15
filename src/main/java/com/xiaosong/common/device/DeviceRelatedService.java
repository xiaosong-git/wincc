package com.xiaosong.common.device;

import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbDevicerelated;

import java.util.List;

public class DeviceRelatedService {
    public static final DeviceRelatedService me = new DeviceRelatedService();

    /**
     * 查询所有的 设备使用记录
     * @return
     */
    public List<TbDevicerelated> findRecordAll() {
        return TbDevicerelated.dao.find("select * from tb_devicerelated");
    }

    /**
     * 保存设备使用记录
     * @param td 设备使用参数
     */
    public boolean save(TbDevicerelated td) {
        return td.save();
    }

    /**
     * 删除设备使用记录
     * @param relatedId  设备编号
     * @return
     */
    public int delete(String relatedId) {
        return Db.delete("delete from tb_devicerelated where id = ?", relatedId);
    }

    /**
     * 修改设备使用记录
     * @param td  设备使用参数
     * @return
     */
    public boolean update(TbDevicerelated td) {
        return td.update();
    }

    /**
     * 根据 设备ip 查询 设备使用记录
     * @param faceIP ip地址
     * @return
     */
    public TbDevicerelated findByFaceIP(String faceIP) {
        return TbDevicerelated.dao.findFirst("select * from tb_devicerelated where faceIP = ? ", faceIP);
    }

    /**
     * 查询设备表的 所有 人脸设备
     * @return
     */
    public List<TbDevice> findAllFaceIp() {
        return TbDevice.dao.find("select deviceIp from tb_device");
    }

    /**
     * 条件查询 设备关联
     * @param faceIP 人脸ip
     * @param qrCodeIP 二维码ip
     * @param relayIP 继电器ip
     */
    public List<TbDevicerelated> findRecord(String faceIP, String qrCodeIP, String relayIP) {
        String sql = "select * from tb_devicerelated";
        if(faceIP!=null){
            sql+=" where faceIP = '"+faceIP+"'";
            if(qrCodeIP!=null){
                sql+=" and QRCodeIP = '"+qrCodeIP+"'";
            }
            if(relayIP!=null){
                sql+=" and relayIP = '"+relayIP+"'";
            }
            return TbDevicerelated.dao.find(sql);
        }
        if(qrCodeIP!=null){
            sql+=" where QRCodeIP = '"+qrCodeIP+"'";
            if(relayIP!=null){
                sql+=" and relayIP = '"+relayIP+"'";
            }
            return TbDevicerelated.dao.find(sql);
        }
        if(relayIP!=null){
            sql+=" where relayIP = '"+relayIP+"'";
            return TbDevicerelated.dao.find(sql);
        }

        return TbDevicerelated.dao.find(sql);
    }
}
