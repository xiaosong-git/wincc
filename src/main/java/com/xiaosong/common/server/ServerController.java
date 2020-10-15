package com.xiaosong.common.server;

import com.jfinal.core.Controller;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.util.RetUtil;
import org.apache.log4j.Logger;

public class ServerController extends Controller {
    private static ServerService srv = ServerService.me;
    private static Logger logger = Logger.getLogger(ServerController.class);

    /**
     * 添加 服务器
     */
    public void floorSave(){
        try {
            String orgCode = getPara("orgCode");
            String pospCode = getPara("pospCode");


            String startDate = getPara("startDate");
            String endDate = getPara("endDate");
            String visitorStartDate = getPara("visitorStartDate");
            String visitorEndDate = getPara("visitorEndDate");
            String stopStartDate = getPara("stopStartDate");
            String stopEndDate = getPara("stopEndDate");
            String qrcodeType = getPara("qrcodeType");


            TbBuildingServer ser = srv.findSer();
            ser.setOrgCode(orgCode);
            ser.setPospCode(pospCode);
            ser.setKey("123456");

            if(ser==null){
                boolean save = srv.save(ser);
                if(save){
                    logger.info("大楼添加成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼添加成功"));
                }else{
                    logger.error("大楼添加失败~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼添加失败"));
                }
            }else{
                boolean update = srv.update(ser);
                if(update){
                    logger.info("大楼修改成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼修改成功"));
                }else{
                    logger.error("大楼修改失败~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼修改失败"));
                }
            }
//            buildingServer.setStartDate(startDate);
//            buildingServer.setEndDate(endDate);
//            buildingServer.setVisitorStartDate(visitorStartDate);
//            buildingServer.setVisitorEndDate(visitorEndDate);
//            buildingServer.setStopStartDate(stopStartDate);
//            buildingServer.setStopEndDate(stopEndDate);
//            buildingServer.setQrcodeType(qrcodeType);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("大楼添加异常~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼添加异常"));
        }
    }

    /**
     * 保存和修改服务器
     */
    public void serverSave(){
        try {
            String serverIp = getPara("serverIp");
            String serverPort = getPara("serverPort");
            String server2Ip = getPara("server2Ip");
            String server2Port = getPara("server2Port");
            TbBuildingServer ser = srv.findSer();
            ser.setServerIp(serverIp);
            ser.setServerPort(serverPort);
            ser.setServer2Ip(server2Ip);
            ser.setServer2Port(server2Port);
            if(ser==null){
                boolean save = srv.save(ser);
                if(save){
                    logger.info("大楼服务器添加成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "服务器添加成功"));
                }else{
                    logger.error("服务器添加失败~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "服务器添加失败"));
                }
            }else{
                boolean update = srv.update(ser);
                if(update){
                    logger.info("服务器修改成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "服务器修改成功"));
                }else{
                    logger.error("服务器修改失败~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "服务器修改失败"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("服务器添加异常~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "服务器添加异常"));
        }
    }

//    /**
//     * 修改大楼服务器
//     */
//    public void update(){
//        try {
//            String serverId = getPara("serverId");
//            String orgCode = getPara("orgCode");
//            String pospCode = getPara("pospCode");
//            String serverIp = getPara("serverIp");
//            String serverPort = getPara("serverPort");
//            String server2Ip = getPara("server2Ip");
//            String server2Port = getPara("server2Port");
//            String startDate = getPara("startDate");
//            String endDate = getPara("endDate");
//            String visitorStartDate = getPara("visitorStartDate");
//            String visitorEndDate = getPara("visitorEndDate");
//            String stopStartDate = getPara("stopStartDate");
//            String stopEndDate = getPara("stopEndDate");
//            String qrcodeType = getPara("qrcodeType");
//
//            TbBuildingServer buildingServer = getModel(TbBuildingServer.class);
//            buildingServer.setServerId(Integer.valueOf(serverId));
//            buildingServer.setOrgCode(orgCode);
//            buildingServer.setPospCode(pospCode);
//            buildingServer.setServerIp(serverIp);
//            buildingServer.setServerPort(serverPort);
//            buildingServer.setServer2Ip(server2Ip);
//            buildingServer.setServer2Port(server2Port);
//            buildingServer.setStartDate(startDate);
//            buildingServer.setEndDate(endDate);
//            buildingServer.setVisitorStartDate(visitorStartDate);
//            buildingServer.setVisitorEndDate(visitorEndDate);
//            buildingServer.setStopStartDate(stopStartDate);
//            buildingServer.setStopEndDate(stopEndDate);
//            buildingServer.setQrcodeType(qrcodeType);
//            boolean save = srv.update(buildingServer);
//            if(save){
//                logger.info("大楼服务器修改成功~");
//                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼服务器修改成功"));
//            }else{
//                logger.error("大楼服务器修改失败~");
//                renderJson(RetUtil.ok(ErrorCodeDef.CODE_ERROR, "大楼服务器修改失败"));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("大楼服务器修改异常~");
//            renderJson(RetUtil.ok(ErrorCodeDef.CODE_ERROR, "大楼服务器修改异常"));
//        }
//    }
//
//    /**
//     * 删除大楼服务器
//     */
//    public void delete(){
//        try {
//            String serverId = getPara("serverId");
//            boolean delete = srv.delete(serverId);
//            if(delete){
//                logger.info("大楼服务器删除成功~");
//                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼服务器删除成功"));
//            }else{
//                logger.error("大楼服务器删除失败~");
//                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼服务器删除失败"));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("大楼服务器删除异常~");
//            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼服务器删除异常"));
//        }
//    }

    /**
     * 查询大楼服务器
     */
    public void index(){
        try {
            TbBuildingServer tbBuildingServer = srv.findSer();
            if(tbBuildingServer!=null){
                logger.info("大楼服务器查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL,tbBuildingServer));
            }else{
                logger.error("大楼服务器查询失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大楼服务器查询失败"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("大楼服务器查询异常~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大楼服务器查询异常"));
        }
    }
}
