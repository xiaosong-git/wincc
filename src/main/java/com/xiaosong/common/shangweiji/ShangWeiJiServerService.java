package com.xiaosong.common.shangweiji;

import com.xiaosong.model.*;

import java.util.Calendar;
import java.util.List;

public class ShangWeiJiServerService {
    public static final ShangWeiJiServerService me = new ShangWeiJiServerService();

    /**
     * 根据 订单号 查询 共享二维码数据
     * @param recirdId
     * @return
     */
    public TbShareroom findByRecordId(int recirdId) {
        return TbShareroom.dao.findFirst("select * from tb_shareroom where recordId = ?",recirdId);
    }

    /**
     * 根据二维码ip地址查询 设备关联表信息
     * @param qrCodeIp
     * @return
     */
    public TbDevicerelated findByQRCodeIP(String qrCodeIp) {
        return TbDevicerelated.dao.findFirst("select * from tb_devicerelated where QRCodeIP = ?", qrCodeIp);
    }

    /**
     * 根据用户id 查询 用户数据
     * @param userId
     * @return
     */
    public TbCompanyuser findByUserId(int userId) {
        return TbCompanyuser.dao.findFirst("select * from tb_companyuser where userId = ?", userId);
    }

    /**
     * 根据 人脸id地址查询 设备关联信息
     * @param qrCodeIp
     * @return
     */
    public TbDevicerelated findByFaceIP(String qrCodeIp) {
        return TbDevicerelated.dao.findFirst("select * from tb_devicerelated where faceIp = ?", qrCodeIp);
    }

    /**
     * 根据设备ip 查询 设备信息
     * @param faceRecogIp
     * @return
     */
    public TbDevice findByDeviceIp(String faceRecogIp) {
        return TbDevice.dao.findFirst("select * from tb_device where deviceIp = ?", faceRecogIp);
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
     * 根据条件查询 访客数据
     * @param soleCode 访客码
     * @param startDateTime 访问开始时间
     * @param endDateTime 访问结束时间
     * @return
     */
    public List<TbVisitor> findByVisitId(String soleCode, String startDateTime, String endDateTime) {
        return TbVisitor.dao.find("select * from tb_visitor where soleCode = ? and startDateTime = ? and endDateTime = ? ", soleCode, startDateTime, endDateTime);
    }


    /**
     * 根据条件查询 访客数据
     * @param startDateTime 访问开始时间
     * @param endDateTime 访问结束时间
     * @return
     */
    public TbVisitor findByVisitDate(String startDateTime, String endDateTime) {
        return TbVisitor.dao.findFirst("select * from tb_visitor where NOW() between ? and ? ", startDateTime, endDateTime);
    }
    /**
     * 根据 用户名 用户身份证号 员工状态 查询 用户数据
     * @param visitorName       用户名
     * @param byVisitorIdCard   用户身份证号
     * @param normal            员工状态
     * @return
     */
    public TbCompanyuser findByNameAndIdNO(String visitorName, String byVisitorIdCard, String normal) {
        List<TbCompanyuser> tbCompanyusers = TbCompanyuser.dao.find("select * from tb_companyuser where userName = ? and idNo = ? and currentStatus = ?", visitorName, byVisitorIdCard, normal);
        if(tbCompanyusers==null||tbCompanyusers.size()==0){
            return null;

        }else{
            return tbCompanyusers.get(0);
        }

    }

    /**
     * 查询大楼信息
     */
    public TbBuildingServer findFloor() {
        return TbBuildingServer.dao.findFirst("select * from tb_building_server");
    }

    /**
     * 全员通行 时间段
     * @param timeT
     * @return
     */
    public TbBuildingServer findAllDate(String timeT) {
        return TbBuildingServer.dao.findFirst("select * from tb_building_server where ? between startDate and endDate",timeT);
    }

    /**
     * 访客通行 时间段
     * @param timeT
     * @return
     */
    public TbBuildingServer findVisitorDate(String timeT) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);//获取到4102当前的小时
        if (hour == 0 || hour == 1 || hour == 2 || hour == 3 || hour == 4 || hour == 5 || hour == 6 || hour == 7 ) {
            return TbBuildingServer.dao.findFirst("select * from tb_building_server where ? between visitorStartDate and '23:59' or ? between '00:00' and visitorEndDate",timeT,timeT);
        } else {
            return TbBuildingServer.dao.findFirst("select * from tb_building_server where ? between visitorStartDate and  visitorEndDate",timeT);
        }
     }

    /**
     * 禁止通行 时间段
     * @param timeT
     * @return
     */
    public TbBuildingServer findStopDate(String timeT) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);//获取到4102当前的小时
        if (hour == 0 || hour == 1 || hour == 2 || hour == 3 || hour == 4 || hour == 5 || hour == 6 || hour == 7 ) {
            return TbBuildingServer.dao.findFirst("select * from tb_building_server where ? between stopStartDate and  '23:59' or ? between '00:00' and stopEndDate",timeT,timeT);
        }else{
            return TbBuildingServer.dao.findFirst("select * from tb_building_server where ? between stopStartDate and stopEndDate",timeT);
        }
    }

    /**
     *  根据访客id 查询 访客数据
     * @param id
     * @return
     */
    public TbVisitor findByVisitorId(Integer id) {
        return TbVisitor.dao.findFirst("select * from tb_visitor where visitId = ?",id);
    }
}
