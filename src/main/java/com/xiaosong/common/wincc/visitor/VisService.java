package com.xiaosong.common.wincc.visitor;

import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.model.TbVisitor;

import java.util.List;

public class VisService {
    public static final VisService me = new VisService();

    /**
     * 根据 访客身份证号码 查询访客信息
     * @param idCard  访客身份证号码
     * @return
     */
    public TbVisitor findVisitorId(String idCard) {
        return TbVisitor.dao.findFirst("select * from tb_visitor where visitorIdCard = ?",idCard);
    }

    /**
     *  根据 下发标识 查询 访客
     * @param isSued  下发成功标识
     * @return
     */
    public List<TbVisitor> findByIssued(String isSued) {
        return TbVisitor.dao.find("select * from tb_visitor where isSued = ?" ,isSued);
    }

    /**
     * 根据 访客id 查询访客数据
     * @param visitorUUID 访客id
     * @return
     */
    public TbVisitor findByUUID(String visitorUUID) {
        return TbVisitor.dao.findFirst("select * from tb_visitor where id = ?", visitorUUID);
    }

    /**
     * 根据下发标识 查询 访客数据
     * @return
     */
    public List<TbVisitor> findByGoneDay() {
        return TbVisitor.dao.find("select * from tb_visitor where NOW() > endDateTime and isSued = '0' and delflag = '1'");
    }

    /**
     * 根据 访客姓名 和访客 身份证 查询 访客数据
     * @param visitorName    访客姓名
     * @param visitorIdCard  访客身份证
     * @return
     */
    public List<TbVisitor> findByVisitor(String visitorName, String visitorIdCard) {
        return TbVisitor.dao.find("select * from tb_visitor where visitorName = ? and visitorIdCard = ? and delFlag = '1'",visitorName,visitorIdCard);
    }

    /**
     * 查询当天访客数量
     */
    public Integer findByVisitorNowCount() {
        return Db.queryInt("select count(*) from tb_visitor where TO_DAYS(visitDate) = TO_DAYS(NOW())");
    }

    /**
     * 查询当日下发人数 访客成功
     * @return
     */
    public Integer findByVisitorNowSuccessCount() {
        return Db.queryInt("select count(*) from tb_visitor where TO_DAYS(visitDate) = TO_DAYS(NOW()) and issued = '0'");
    }

    /**
     * 查询当日下发人数 访客失败
     * @return
     */
    public Integer findByVisitorNowFailCount() {
        return Db.queryInt("select count(*) from tb_visitor where TO_DAYS(visitDate) = TO_DAYS(NOW()) and issued = '1'");
    }
}
