package com.xiaosong.common.wincc.companyuser;


import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.util.HttpUtil;
import com.xiaosong.util.ThirdResponseObj;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;

import java.util.List;

public class StaffService {
    private static Logger logger = Logger.getLogger(StaffService.class);
    public static final StaffService me = new StaffService();

    /**
     * 根据身份证号码 查询用户
     * @param idCard 身份证号码
     * @return
     */
    public TbCompanyuser findOne(Integer idCard) {
        return TbCompanyuser.dao.findFirst("select * from tb_companyuser where idNO = ? ", idCard);
    }

    /**
     * 根据 用户名 用户身份证号 员工状态 查询 用户数据
     * @param visitorName       用户名
     * @param byVisitorIdCard   用户身份证号
     * @param normal            员工状态
     * @return
     */
    public TbCompanyuser findByNameAndIdNO(String visitorName, String byVisitorIdCard, String normal) {
        List<TbCompanyuser> tbCompanyusers = TbCompanyuser.dao.find("select * from tb_companyuser where userName = ? and idNO = ? and currentStatus = ?", visitorName, byVisitorIdCard, normal);
        if(tbCompanyusers==null||tbCompanyusers.size()==0){
            return null;

        }else{
            return tbCompanyusers.get(0);
        }

    }

    /**
     * 根据 接收时间 查询 用户数据
     * @param date 接收时间
     * @return
     */
    public List<TbCompanyuser> findBeforeToDay(String date) {
        return TbCompanyuser.dao.find("select * from tb_companyuser where receiveDate = ? and isSued = '1' and currentStatus = 'normal'", date);
//        return TbCompanyuser.dao.find("select * from tb_companyuser where isSued = '1'");
    }

    /**
     * 查询 未删除 并且 下发成功 还是离职状态的 用户数据
     * @return
     */
    public List<TbCompanyuser> findFailDel() {
        return TbCompanyuser.dao.find("select * from tb_companyuser where isDel = '1' and isSued = '0' and currentStatus = 'delete'");
    }

    /**
     * 删除 用户
     * @param companyuser 用户参数
     */
    public void deleteOne(TbCompanyuser companyuser) {
        companyuser.delete();
    }

    /**
     * 删除下发的 数据
     * @param deviceIp 设备ip
     * @param name     用户名
     * @param idNO     身份证号码
     * @return
     * @throws Exception
     */
    public boolean sendDelWhiteList(String deviceIp, String name, String idNO) throws Exception {
        boolean allSuccess = true;
        JSONObject paramsJson = new JSONObject();
        String URL = "http://" + deviceIp + ":8080/office/addOrDelUser";
        //System.out.println(name+"///////////"+idNO);
        paramsJson.put("name", name);
        paramsJson.put("idCard", idNO);
        paramsJson.put("op", "delete");

        StringEntity entity = new StringEntity(paramsJson.toJSONString(), "UTF-8");
        ThirdResponseObj thirdResponseObj = null;
        entity.setContentType("aaplication/json");
        try {
            thirdResponseObj = HttpUtil.http2Se(URL, entity, "UTF-8");
        } catch (Exception e) {
            allSuccess = false;
            //e.sendLog(towerInforService.findOrgId());
            logger.error(deviceIp+"删除访客过期数据失败");
            //e.printStackTrace();
        }
        if(thirdResponseObj == null) {
            logger.error("人脸识别仪器" + deviceIp + "接收失败");
            return false;
        }
        if ("success".equals(thirdResponseObj.getCode())) {
            logger.warn("人脸识别仪器" + deviceIp + "接收成功");
            logger.info("人脸识别仪器" + deviceIp + "接收成功");
        } else {
            logger.error("人脸识别仪器" + deviceIp + "接收失败");
            allSuccess = false;
        }
        return allSuccess;
    }

    public void delete() {
        Db.update("UPDATE tb_companyuser SET expt = null");
    }

    /**
     *  查询当天员工数量
     */
    public Integer findByStaffNowCount() {
        return Db.queryInt("select count(*) from tb_companyuser where TO_DAYS(receiveDate) = TO_DAYS(NOW())");
    }

    /**
     *  当前上位机总员工数
     * @return
     */
    public Integer findByStaffCount() {
        return Db.queryInt("select count(*) from tb_companyuser");
    }

    /**
     * 查询 当日删除人数
     * @return
     */
    public Integer findByStaffDelCount() {
        return Db.queryInt("select count(*) from tb_companyuser where TO_DAYS(receiveDate) = TO_DAYS(NOW()) and isdel='0'");
    }

    /**
     * 查询 当日下发人数 员工成功
     * @return
     */
    public Integer findByStaffNowSuccessCount() {
        return Db.queryInt("select count(*) from tb_companyuser where TO_DAYS(receiveDate) = TO_DAYS(NOW()) and issued = '0'");
    }

    /**
     * 查询 当日下发人数 员工失败
     * @return
     */
    public Integer findByStaffNowFailCount() {
        return Db.queryInt("select count(*) from tb_companyuser where TO_DAYS(receiveDate) = TO_DAYS(NOW()) and issued = '1'");
    }
}
