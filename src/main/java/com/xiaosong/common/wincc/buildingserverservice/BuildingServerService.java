package com.xiaosong.common.wincc.buildingserverservice;

import com.xiaosong.model.TbBuildingServer;

public class BuildingServerService {
    public  static final BuildingServerService me = new BuildingServerService();

    static final TbBuildingServer dao = TbBuildingServer.dao;

    public TbBuildingServer findInfo(){
        return dao.findFirst("select * from tb_building_server");
    }

    public String findByOrgCode(){
        TbBuildingServer tbBuildingServer = dao.findFirst("select * from tb_building_server");
        return tbBuildingServer.getStr("orgCode");
    }

    public String findPospCode(){
        TbBuildingServer tbBuildingServer = dao.findFirst("select * from tb_building_server");
        return tbBuildingServer.getStr("pospCode");
    }

}
