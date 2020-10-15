package com.xiaosong.common.device;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.xiaosong.model.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DeviceService {
    public static final DeviceService me = new DeviceService();

    public boolean save(TbDevice tbDevice) {
        return tbDevice.save();
    }

    /**
     * 根据ip查询设备
     *
     * @param faceIP 设备ip
     * @return
     */
    public TbDevice findByDeviceIp(String faceIP) {
        List<TbDevice> list = TbDevice.dao.find("select * from tb_device WHERE deviceIp = ?", faceIP);
        if (list.size() <= 0 || list == null) {
            return null;
        }
        return list.get(0);
    }


    /**
     * 保存通行记录
     *
     * @param accessRecord 通行数据
     */
    public void saveAccessrecord(TbAccessrecord accessRecord) {
        accessRecord.save();
    }

    /**
     * 根据 时间  身份证号码 ,姓名 查询 通行记录
     *
     * @param time   时间
     * @param idCard 身份证号码
     * @param name   姓名
     * @return
     */
    public List<TbAccessrecord> findAccessrecord(String time, String idCard, String name) {
        return TbAccessrecord.dao.find("select * from tb_accessrecord where scanTime = ? and idCard = ? and userName = ?", time, idCard, name);
    }

    /**
     * 根据userId查询用户名的数据
     *
     * @param userId
     * @return
     */
    public TbCompanyuser findByUserId(int userId) {
        List<TbCompanyuser> list = TbCompanyuser.dao.find("select * from tb_companyuser where userId = ?", userId);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 根据身份证号查询 访问者信息
     *
     * @param idCardNo 身份证号
     * @return
     */
    public TbVisitor findVisitorId(String idCardNo) {
        List<TbVisitor> list = TbVisitor.dao.find("select * from tb_visitor where userId  = ?", idCardNo);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }

    }

    /**
     * 根据条件查询所有的 访问者
     *
     * @param userrealname 访问人姓名
     * @param idNO         身份证号
     * @param dateTime     访问时间
     * @return
     */
    public List<TbVisitor> findByBetweenTime(String userrealname, String idNO, String dateTime) {
        return TbVisitor.dao.find("SELECT * from tb_visitor where visitorName= ? AND visitorIdCard= ? and ? between preStartTime and endDateTime", userrealname, idNO, dateTime);
    }

    /**
     * 根据用户id 查询 共享数据表
     *
     * @param userId 用户id
     * @return
     */
    public TbShareroom findByUser(int userId) {
        List<TbShareroom> list = TbShareroom.dao.find("select * from tb_shareRoom where userId = ?", userId);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 修改设备
     *
     * @param tbDevice 设备
     * @return
     */
    public boolean update(TbDevice tbDevice) {
        return tbDevice.update();
    }

    /**
     * 根据设备ip查询 设备
     *
     * @param deviceId 设备ip
     * @return
     */
    public int deleteDevice(String deviceId) {
        return Db.delete("delete from tb_device where deviceId = ?", deviceId);
    }

    /**
     *  所有设备
     * @return
     */
    public List<TbDevice> findDevice() {
        return TbDevice.dao.find("select * from tb_device ");
    }

    /**
     * 查询所有的设备
     *
     * @return
     */
    public List<TbDevice> findDeviceAll() {
        return TbDevice.dao.find("select * from tb_device");
    }

    /**
     * 根据条件 查询 设备
     *
     * @param deviceType @return
     * @param deviceIp
     * @param deviceMode
     */
    public List<TbDevice> findByDevice(String deviceType, String deviceIp, String deviceMode) {
        String sql = "select * from tb_device";
        if (deviceType != null) {
            sql += " where deviceType = '" + deviceType + "'";
            if (deviceIp != null) {
                sql += " and deviceIp = '" + deviceIp + "'";
            }
            if (deviceMode != null) {
                sql += " and deviceMode = '" + deviceMode + "'";
            }
            return TbDevice.dao.find(sql);
        }
        if (deviceIp != null) {
            sql += " where deviceIp = '" + deviceIp + "'";
            if (deviceMode != null) {
                sql += " and deviceMode = '" + deviceMode + "'";
            }
            return TbDevice.dao.find(sql);
        }
        if (deviceMode != null) {
            sql += " where deviceMode = '" + deviceMode + "'";
            return TbDevice.dao.find(sql);
        }
        return TbDevice.dao.find(sql);
    }

    /**
     * 根据设备类型 查询 设备
     *
     * @param currentPage
     * @param pageSize
     * @param deviceType  @return
     */
    public Page<Record> findByType(int currentPage, int pageSize, String deviceType) {
        return Db.paginate(currentPage, pageSize, "select *", "from tb_device where deviceType = ? ", deviceType);
    }

    /**
     * 根据ip查询设备信息
     *
     * @param currentPage
     * @param pageSize
     * @param deviceIp
     * @return
     */
    public Page<Record> findByIp(int currentPage, int pageSize, String deviceIp) {
        return Db.paginate(currentPage, pageSize, "select *", "from tb_device where deviceIp = ? ", deviceIp);
    }

    public List<TbDevicerelated> findFIPbyFloor(String companyFloor){
        return TbDevicerelated.dao.find("select * from tb_devicerelated where contralFloor LIKE CONCAT('%|',?,'|%') and faceIP !=''",companyFloor);
    }

    /**
     * 根据楼层 查询 设备
     * @param companyFloor
     * @return
     */
    public List<String> getAllFaceDeviceIP(String companyFloor) {
        List<String> allFaceIP = new ArrayList<String>();
        //楼层为空，下发全部设备
        if (companyFloor ==null||companyFloor==""||"undefined".equals(companyFloor)||companyFloor.length()==0) {
            List<TbDevicerelated> list = TbDevicerelated.dao.findAll();
            if(list.size() > 0){
                for (int i = 0; i < list.size(); i++) {
                    String faceIp = list.get(i).getFaceIP();
                    allFaceIP.add(faceIp);
                }
            }
        }else{
            //对应多楼层设备
            if (companyFloor.contains("|")) {
                String[] floors = companyFloor.split("\\|");
                for (String floor : floors) {
                    List<TbDevicerelated> list = findFIPbyFloor(floor);
                    for (int i = 0; i < list.size(); i++) {
                        String faceIp =  list.get(i).getFaceIP();

                        if (!allFaceIP.contains(faceIp)) {
                            allFaceIP.add(faceIp);
                        }
                    }
                }
            }else{
                List<TbDevicerelated> list = findFIPbyFloor(companyFloor);
                for (int i = 0; i < list.size(); i++) {
                    String faceIp =  list.get(i).getFaceIP();

                    if (!allFaceIP.contains(faceIp)) {
                        allFaceIP.add(faceIp);
                    }
                }
            }
        }
        return allFaceIP;
    }

//    /**
//     * 根据通行权限 查询设备
//     *
//     * @param contralFloor
//     * @return
//     */
//    public List<String> getAllFaceDeviceIP(String contralFloor) {
//        List<String> allFaceIP = new ArrayList<String>();
//        if("".equals(contralFloor)||contralFloor==null){
//            List<TbDevicerelated> list = TbDevicerelated.dao.find("select * from tb_devicerelated where faceIP !=''");
//            if (list.size() > 0) {
//                for (TbDevicerelated devicerelated : list) {
//                    TbDevice device = findByDeviceIp(devicerelated.getFaceIP());
//                    if (!StringUtils.isEmpty(device.getDeviceType())) {
//                        allFaceIP.add(device.getDeviceIp());
//                    }
//                }
//            }
//        }else {
//            List<TbDevicerelated> list = TbDevicerelated.dao.find("select * from tb_devicerelated  where contralFloor LIKE CONCAT('%|'," + contralFloor + ",'|%') and faceIP !=''");
//            if (list.size() > 0) {
//                for (TbDevicerelated devicerelated : list) {
//                    TbDevice device = findByDeviceIp(devicerelated.getFaceIP());
//                    if (!StringUtils.isEmpty(device.getDeviceType())) {
//                        allFaceIP.add(device.getDeviceIp());
//                    }
//                }
//                //for (int i = 0; i < list.size(); i++) {
//                //    String faceIp = ((TbFailreceive) list.get(i)).getFaceIp();
//                //    TbDevice device = findByDeviceIp(faceIp);
//                //    if (!StringUtils.isEmpty(device.getDeviceType())) {
//                //        allFaceIP.add(faceIp);
//                //    }
//                //}
//            }
//        }
//        return allFaceIP;
//    }

    /**
     * 删除 多余重复的 数据
     */
    public void deleteSurplus() {
        Db.delete("delete from tb_accessrecord where id not in (select id from (select * from tb_accessrecord GROUP BY scanDate,scanTime) t) ");
    }

    /**
     * 根据下发标识 查询 通行记录
     *
     * @param isSendFlag 下发标识
     * @return
     */
    public List<TbAccessrecord> findByIsSendFlag(String isSendFlag) {
        return TbAccessrecord.dao.find("select * from tb_accessrecord where isSendFlag = ?", isSendFlag);
    }

    /**
     * 根据 开始id 和结束 id 修改通行记录
     *
     * @param startId 开始id
     * @param endId   结束 id
     */
    public void updateSendFlag(Integer startId, Integer endId) {
        Db.update("update tb_accessrecord set isSendFlag = 'T' where id between ? and ?", startId, endId);
    }

    /**
     *  根据 设备ip查询 设备信息
     * @param faceIP
     * @return
     */
    public TbDevice findDeviceIp(String faceIP) {
        return TbDevice.dao.findFirst("select * from tb_device WHERE deviceIp = ?", faceIP);
    }

    /**
     *  修改 下发失败表信息
     */
    public void UpdateDownNum() {
        Db.update("update tb_failreceive set downNum = 0");
    }
}
