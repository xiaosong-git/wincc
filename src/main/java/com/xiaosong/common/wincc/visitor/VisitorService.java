package com.xiaosong.common.wincc.visitor;

import com.xiaosong.model.TbVisitor;

import java.util.List;

public class VisitorService {
    public static final VisitorService me = new VisitorService();

    static final TbVisitor dao = TbVisitor.dao;

    public void save(TbVisitor tbVisitor){
        tbVisitor.save();
    }
    public TbVisitor findVisitorId(String userId){
        return dao.findFirst("select * from tb_visitor where userId = ?",userId);
    }

    public List<TbVisitor> findByBetweenTime(String userName,String idNO,String nowTime){
        List<TbVisitor> list= dao.find("select * from tb_visitor where visitorName = ? and visitorIdCard = ? and  ? between startDateTime and endDateTime",userName,idNO,nowTime);
        return list;
    }

    public List<TbVisitor> findByIssued(String issued){
        return dao.find("select * from tb_visitor where issued = ?",issued);
    }
    public TbVisitor findByUUID(String UUID){
        return dao.findFirst("select * from tb_visitor where id = ?",UUID);

    }
}
