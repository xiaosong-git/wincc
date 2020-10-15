package com.xiaosong.common.device;

import com.jfinal.core.Controller;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbDevicerelated;
import com.xiaosong.util.RetUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备使用配置
 */
public class DeviceRelatedController extends Controller {
    public DeviceRelatedService srv = DeviceRelatedService.me;
    private static Logger logger = Logger.getLogger(DeviceRelatedController.class);


    /**
     * 查询设备关联信息
     */
    public void index() {
        try {
            String faceIP = getPara("faceIP");
            String QRCodeIP = getPara("QRCodeIP");
            String relayIP = getPara("relayIP");
            int page = Integer.parseInt(getPara("currentPage"));  //当前页
            int number = Integer.parseInt(getPara("pageSize"));   //一页显示数量
            int index = (page - 1) * number;
            List<TbDevicerelated> list = new ArrayList<>();

            List<TbDevicerelated>  devicerelateds = null;
            if (faceIP == null && QRCodeIP == null && relayIP == null) {
                devicerelateds = srv.findRecordAll();
                for (int i = index; i < devicerelateds.size() && i < (index + number); i++) {
                    list.add(devicerelateds.get(i));
                }
            }else{
                List<TbDevicerelated> record = srv.findRecord(faceIP, QRCodeIP, relayIP);
                for (int i = index; i < record.size() && i < (index + number); i++) {
                    list.add(record.get(i));
                }
            }
            if (list.size()>0) {
                logger.info("设备关联信息查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, list.size()));
            } else {
                logger.info("设备关联信息查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, list.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设备关联查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备关联查询异常~"));
        }
    }

    /**
     * 保存设备关联配置
     */
    public void save() {
        //String relatedId = getPara("relatedId");  //关联编号(自增)（主键1）
        String faceIP = getPara("faceIP");        //人脸设备IP地址
        String QRCodeIP = getPara("QRCodeIP");    //读头IP地址
        String relayIP = getPara("relayIP");      //继电器IP地址
        //String relayPort = getPara("addr");  //继电器端口
        String relayOUT = getPara("relayOUT");    //继电器电源输出口
        String contralFloor = getPara("contralFloor");  //对应的控制楼层
        String turnOver = getPara("turnOver");     //进出标识（in/out）

        TbDevicerelated td = getModel(TbDevicerelated.class);
        td.setFaceIP(faceIP);
        td.setQRCodeIP(QRCodeIP);
        td.setRelayIP(relayIP);
        td.setRelayPort(String.valueOf(8080));
        td.setRelayOUT(relayOUT);
        td.setContralFloor(contralFloor);
        td.setTurnOver(turnOver);

        boolean save = srv.save(td);
        if (save) {
            logger.info("设备使用添加成功");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "设备使用添加成功"));
        } else {
            logger.info("设备使用添加失败");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备使用添加失败"));
        }
    }

    /**
     * 修改设备使用记录
     */
    public void update() {
        try {
            String relatedId = getPara("relatedId");  //关联编号(自增)（主键1）
            String faceIP = getPara("faceIP");        //人脸设备IP地址
            String QRCodeIP = getPara("QRCodeIP");    //读头IP地址
            String relayIP = getPara("relayIP");      //继电器IP地址
            //String relayPort = getPara("relayPort");  //继电器端口
            String relayOUT = getPara("relayOUT");    //继电器电源输出口
            String contralFloor = getPara("contralFloor");  //对应的控制楼层
            String turnOver = getPara("turnOver");     //进出标识（in/out）

            TbDevicerelated td = getModel(TbDevicerelated.class);
            td.setId(Integer.valueOf(relatedId));
            td.setFaceIP(faceIP);
            td.setQRCodeIP(QRCodeIP);
            td.setRelayIP(relayIP);
            td.setRelayPort(String.valueOf(8080));
            td.setRelayOUT(relayOUT);
            td.setContralFloor(contralFloor);
            td.setTurnOver(turnOver);
            boolean update = srv.update(td);
            if (update) {
                logger.info("设备使用修改成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "设备使用修改成功~"));
            } else {
                logger.error("设备使用修改失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备使用修改失败~"));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logger.error("设备使用异常!");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备使用异常!"));
        }

    }

    /**
     * 查找所有的 人脸ip
     */
    public void allIp() {
        try {
            List<TbDevice> tbDevices = srv.findAllFaceIp();
            if (tbDevices != null) {
                logger.info("设备ip查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, tbDevices, tbDevices.size()));
            } else {
                logger.error("设备ip查询失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备ip查询失败"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设备ip查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备ip查询异常"));
        }
    }

    /**
     * 删除设备使用记录
     */
    public void delete() {
        try {
            //前端传入参数
            String relatedId = getPara("relatedId");  //关联编号(自增)（主键1）
            int delete = srv.delete(relatedId);
            if (delete == 1) {
                logger.info("删除设备使用成功.");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "删除设备使用成功"));
            } else {
                logger.error("删除设备使用失败!");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除设备使用失败!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设备使用异常!");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备使用异常!"));
        }
    }
}