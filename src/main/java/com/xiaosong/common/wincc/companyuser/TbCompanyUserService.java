package com.xiaosong.common.wincc.companyuser;

import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.model.TbCompanyuser;

import java.util.List;

public class TbCompanyUserService {

    public static final	TbCompanyUserService me = new TbCompanyUserService();

    static final TbCompanyuser dao = TbCompanyuser.dao;

    public TbCompanyuser findByUserId(int userId){
       return dao.findFirst("select * from tb_companyuser where userId = ?",userId);
    }

    public TbCompanyuser findByNameAndIdNO(String name,String idNO,String status){
       return dao.findFirst("select * from tb_companyuser where userName = ? and idNO = ? and currentStatus = ?",name,idNO,status);
    }

    public void delALL(){
        Db.delete("delete from tb_companyuser");
    }

    public List<TbCompanyuser> findByIsSued(){
        return dao.find("select * from tb_companyuser where  isSued = '1'");
    }

    public List<TbCompanyuser> findBeforeToDay(String today){
        return dao.find("select * from tb_companyuser where receiveDate < ? and isSued = '1' and  currentStatus = 'normal'",today);
    }

    public List<TbCompanyuser> findFailDel(){
        return dao.find("select * from tb_companyuser where  isSued = '0' and isDel = '1' and currentStatus = 'deleted'");
    }
}
