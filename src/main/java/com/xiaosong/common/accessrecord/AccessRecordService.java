package com.xiaosong.common.accessrecord;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.xiaosong.model.TbAccessrecord;

import java.util.List;

public class AccessRecordService {
    public static final AccessRecordService me = new AccessRecordService();


    /**
     *  查询 全部通行表
     */
    public Page<Record> findAll(int currentPage, int pageSize) {
        return Db.paginate(currentPage, pageSize, "select *", "from tb_accessrecord order BY scanDate desc,scanTime desc ");
    }

    public List<TbAccessrecord> findByBim(String userName, String userType, String turnOver, String deviceIp, String beginDate, String endDate) {
//        Map<String,Object> map = new HashMap<>();
//        map.put("userName",userName);
//        map.put("cardNO",cardNo);
//        map.put("deviceIp",deviceIp);
//        map.put("beginDate",beginDate);
//        map.put("endDate",endDate);
//        SqlPara sqlPara = Db.getSqlPara("access.findByBim", map);
//        return TbAccessrecord.dao.find(sqlPara);
        String sql = " select * from (select * from tb_accessrecord order BY scanDate desc,scanTime desc )t ";
        if (userName != null) {
            sql += " where t.userName like '%" + userName + "%' ";
            if (userType != null) {
                sql += " and t.userType = '" + userType + "'";
            }
            if(turnOver != null){
                sql += " and t.turnOver = '" + turnOver + "'";
            }
            if (deviceIp != null) {
                sql += " and t.deviceIp = '" + deviceIp + "'";
            }
            if (beginDate != null) {
                sql += " and t.scanDate >= '" + beginDate + "'";
            }
            if (endDate != null) {
                sql += " and t.scanDate <= '" + endDate + "'";
            }
            return TbAccessrecord.dao.find(sql);
        }
        if (userType != null) {
            sql += " where t.userType = '" + userType + "'";
            if(turnOver != null){
                sql += " and t.turnOver = '" + turnOver + "'";
            }
            if (deviceIp != null) {
                sql += " and t.deviceIp = '" + deviceIp + "'";
            }
            if (beginDate != null) {
                sql += " and t.scanDate >= '" + beginDate + "'";
            }
            if (endDate != null) {
                sql += " and t.scanDate <= '" + endDate + "'";
            }
            return TbAccessrecord.dao.find(sql);
        }
        if(turnOver != null){
            sql += " where t.turnOver = '" + turnOver + "'";
            if (deviceIp != null) {
                sql += " and t.deviceIp = '" + deviceIp + "'";
            }
            if (beginDate != null) {
                sql += " and t.scanDate >= '" + beginDate + "'";
            }
            if (endDate != null) {
                sql += " and t.scanDate <= '" + endDate + "'";
            }
            return TbAccessrecord.dao.find(sql);
        }
        if(deviceIp!=null){
            sql+= " where t.deviceIp = '" + deviceIp + "'";
            if (beginDate != null) {
                sql += " and t.scanDate >= '" + beginDate + "'";
            }
            if (endDate != null) {
                sql += " and t.scanDate <= '" + endDate + "'";
            }
            return TbAccessrecord.dao.find(sql);
        }
        if(beginDate!=null){
            sql+=" where t.scanDate >= '" + beginDate + "'";
            if (endDate != null) {
                sql += " and t.scanDate <= '" + endDate + "'";
            }
            return TbAccessrecord.dao.find(sql);
        }
        if(endDate!=null){
            sql+=" where t.scanDate <= '" + endDate + "'";

            return TbAccessrecord.dao.find(sql);
        }
        return TbAccessrecord.dao.find(sql);
    }

    /**
     *  查询通行表中的 总数
     * @return
     */
    public int selectCount() {
        return Db.queryInt("select count(*) from tb_accessrecord");
    }

    /**
     * 查找 人脸设备的 通行人数
     * @param date
     */
    public Integer findFace(String date) {
        return Db.queryInt("select count(*) from tb_accessrecord where deviceType = 'FACE' and scanDate = ?",date);
    }

    /**
     * 查找 二维码设备的 通行人数
     */
    public Integer findQRCode(String date) {
        return Db.queryInt("select count(*) from tb_accessrecord where deviceType = 'QRCODE' and scanDate = ?",date);
    }

    /**
     * 查找 员工类型的 通行人数
     */
    public Integer findStaff(String date) {
        return Db.queryInt("select count(*) from tb_accessrecord where userType = 'staff' and scanDate = ?",date);
    }

    /**
     * 查找 访客类型的 通行人数
     */
    public Integer findVisitor(String date) {
        return Db.queryInt("select count(*) from tb_accessrecord where userType = 'visitor' and scanDate = ?",date);
    }

//    public Integer findStaff(String date) {
//        return Db.queryInt("select count(*) from tb_accessrecord where scanDate = ? and userType='staff' ",date);
//    }
//
//    public Integer findVisitor(String date) {
//        return Db.queryInt("select count(*) from tb_accessrecord where scanDate = ? and userType='visitor' ",date);
//    }
}
