package com.xiaosong.common.server;

import com.xiaosong.model.TbBuildingServer;

public class ServerService {
    public static final ServerService me = new ServerService();

    /**
     * 保存大楼服务器
     *
     * @param buildingServer
     */
    public boolean save(TbBuildingServer buildingServer) {
        return buildingServer.save();
    }

    /**
     * 修改大楼服务器
     *
     * @param buildingServer
     * @return
     */
    public boolean update(TbBuildingServer buildingServer) {
        return buildingServer.update();
    }

    /**
     * 删除大楼服务器
     *
     * @param serverId
     */
    public boolean delete(String serverId) {
        return TbBuildingServer.dao.deleteById(serverId);
    }

    /**
     * 查询 大楼编号
     */
    public String findByOrgCode() {
        TbBuildingServer tbBuilding = TbBuildingServer.dao.findFirst("select orgCode from tb_building_server");
        if (tbBuilding == null) {
            return null;
        } else {
            return tbBuilding.getOrgCode();
        }
    }

    /**
     * 查询 大楼 服务器ip
     *
     * @return
     */
    public String findBySerIp() {
        TbBuildingServer tbBuilding = TbBuildingServer.dao.findFirst("select serverIp from tb_building_server");
        if (tbBuilding != null) {
            return tbBuilding.getServerIp();
        } else {
            return null;
        }
    }

    /**
     * 查询服务器端口
     *
     * @return
     */
    public String findBySerPort() {
        TbBuildingServer tbBuilding = TbBuildingServer.dao.findFirst("select serverPort from tb_building_server");
        if (tbBuilding != null) {
            return tbBuilding.getServerPort();
        } else {
            return null;
        }
    }

    /**
     * 查询上位机编码
     *
     * @return
     */
    public String findPospCode() {
        TbBuildingServer tbBuilding = TbBuildingServer.dao.findFirst("select pospCode from tb_building_server");
        if (tbBuilding != null) {
            return tbBuilding.getPospCode();
        } else {
            return null;
        }
    }

    /**
     * 查询key
     *
     * @return
     */
    public String findByKey() {
        TbBuildingServer tbBuilding = TbBuildingServer.dao.findFirst("select key from tb_building_server");
        if (tbBuilding != null) {
            return tbBuilding.getKey();
        } else {
            return null;
        }
    }

    /**
     * 查询大楼服务器信息
     *
     * @return
     */
    public TbBuildingServer findSer() {
        return TbBuildingServer.dao.findFirst("select * from tb_building_server");
    }


    /**
     * 查询大楼中的 联网方式
     *
     * @return
     */
    public TbBuildingServer findNetType() {
        return TbBuildingServer.dao.findFirst("select * from tb_building_server");
    }
}
