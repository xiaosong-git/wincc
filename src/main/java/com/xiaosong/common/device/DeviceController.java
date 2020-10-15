package com.xiaosong.common.device;

import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.lib.ToolKits;
import com.dhnetsdk.module.LoginModule;
import com.jfinal.core.Controller;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.xiaosong.common.websocket.WebSocket;
import com.xiaosong.config.InitDevice;
import com.xiaosong.constant.Constant;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbDevice;
import com.xiaosong.sdkConfig.HCNetSDK;
import com.xiaosong.sdkConfig.HCNetSDKService;
import com.xiaosong.util.RetUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备管理
 */
public class DeviceController extends Controller {
    private DeviceService srv = DeviceService.me;
    private static Logger logger = Logger.getLogger(DeviceController.class);
    private WebSocket webSocket = new WebSocket();

    /**
     * 添加 设备
     */
    public void save() {
        try {
            String deviceType = getPara("deviceType");  //设备类型
            String deviceMode = getPara("deviceMode");  //设备模式
            String deviceIp = getPara("deviceIP");      //设备ip
//            String devicePort = getPara("devicePort");  //设备端口
            String status = getPara("status");          //设备状态
            String admin = getPara("deviceName");            //设备登录账号
            String password = getPara("devicePassword");      //设备登录密码

            TbDevice tbDevice = getModel(TbDevice.class);
            tbDevice.setDeviceType(deviceType);
            tbDevice.setDeviceMode(deviceMode);
            tbDevice.setDeviceIp(deviceIp);
            tbDevice.setStatus(Integer.valueOf(status));
            tbDevice.setDeviceName(admin);
            tbDevice.setDevicePassword(password);

            //保存设备
            boolean save = srv.save(tbDevice);
            if (save) {
                logger.info("设备添加成功,开始 长连接..");
                if (deviceType.equals("DS-K5671")) {
                    if(!Constant.isInitHc){
                        InitDevice.initHc();
                    }
                    HCNetSDKService accessRecord = HCNetSDKService.me;
                    logger.info("长连接" + deviceIp);
                    accessRecord.sendAccessRecord(deviceIp);

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (deviceType.equals("DH-ASI728")) {
                    //初始化大华设备
                    if(!Constant.isInitDh){
                        InitDevice.initDh();
                    }
                    HCNetSDKService accessRecord = HCNetSDKService.me;
                    logger.info("长连接" + deviceIp);
                    accessRecord.dhSendAccessRecord(deviceIp);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    }
                }
                logger.info("设备添加成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "设备添加成功~"));
            } else {
                logger.error("设备添加失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备添加失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设备添加异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备添加异常~"));
        }
    }

    /**
     * 修改设备
     */
    public void update() {
        try {
            String deviceId = getPara("deviceId");     //设备id主键 根据 id修改设备
            String deviceType = getPara("deviceType"); //设备类型
            String deviceMode = getPara("deviceMode"); //设备模式
            String deviceIp = getPara("deviceIp");     //设备ip
//            String devicePort = getPara("devicePort"); //设备端口
            String status = getPara("status");         //设备状态
            String admin = getPara("deviceName");           //设备登录账号
            String password = getPara("devicePassword");     //设备登录密码

            TbDevice tbDevice = getModel(TbDevice.class);
            tbDevice.setDeviceType(deviceType);
            tbDevice.setDeviceId(Integer.valueOf(deviceId));
            tbDevice.setDeviceMode(deviceMode);
            tbDevice.setDeviceIp(deviceIp);
            tbDevice.setStatus(Integer.valueOf(status));
//            tbDevice.setDevicePort(Integer.valueOf(devicePort));
            tbDevice.setDeviceName(admin);
            tbDevice.setDevicePassword(password);
            boolean update = srv.update(tbDevice);
            if (update) {
                if (deviceType.equals("DS-K5671")) {
                    if(!Constant.isInitHc){
                        InitDevice.initHc();
                    }
                    HCNetSDKService accessRecord = HCNetSDKService.me;
                    logger.info("长连接" + deviceIp);
                    accessRecord.sendAccessRecord(deviceIp);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (deviceType.equals("DH-ASI728")) {
                    if(!Constant.isInitDh){
                        InitDevice.initDh();
                    }
                    //初始化大华设备
                    HCNetSDKService accessRecord = HCNetSDKService.me;
                    logger.info("长连接" + deviceIp);
                    accessRecord.dhSendAccessRecord(deviceIp);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                logger.info("设备修改成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "设备修改成功~"));
            } else {
                logger.error("设备修改失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备修改失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("设备修改异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备修改异常~"));
        }

    }

    /**
     * 删除设备
     */
    public void delete() {
        try {
            //获取前台数据
            String deviceId = getPara("deviceId");  //设备id主键 根据 id删除设备
            int i = srv.deleteDevice(deviceId);
            if (i == 1) {
                logger.info("删除设备信息成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "删除设备信息成功~"));
            } else {
                logger.error("删除设备信息失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除设备信息失败~"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("删除设备信息异常");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除设备信息异常"));
        }
    }

    /**
     * 批量删除
     */
    public void batchDel() {
        try {
            //获取前台数据
            String deviceId = getPara("deviceId"); //设备id主 键 根据 id删除设备
            String[] split = deviceId.split(",");
            for (String id : split) {
                int i = srv.deleteDevice(id);
                if (i == 1) {
                    logger.info("批量删除设备信息成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "批量删除设备信息成功~"));
                } else {
                    logger.error("批量删除设备信息失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "批量删除设备信息失败~"));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            logger.error("批量删除设备信息异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "批量删除设备信息异常~"));
        }
    }

    /**
     * 条件查询
     */
    public void index() {
        try {
            String deviceType = getPara("deviceType");                  //设备类型
            String deviceMode = getPara("deviceMode");                  //设备型号
            String deviceIp = getPara("deviceIP");                      //设备ip
            int page = Integer.parseInt(getPara("currentPage"));  //当前页
            int number = Integer.parseInt(getPara("pageSize"));   //一页显示数量
            int index = (page - 1) * number;
            List<TbDevice> list = new ArrayList<>();

            List<TbDevice> devices = null;
            if (deviceIp == null && deviceMode == null && deviceType == null) {
                devices = srv.findDeviceAll();
                for (int i = index; i < devices.size() && i < (index + number); i++) {
                    list.add(devices.get(i));
                }
            } else {
                devices = srv.findByDevice(deviceType, deviceIp, deviceMode);
                for (int i = index; i < devices.size() && i < (index + number); i++) {
                    list.add(devices.get(i));
                }
            }
            if (devices.size() > 0) {
                logger.info("设备查询成功~");
                renderText("code=0000");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, list.size()));
            } else {
                logger.info("设备查询成功~");
                renderText("code=0000");

//                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL,list,  list.size()));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logger.error("设备查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "设备查询异常"));
        }
    }

    public void open() {
        try {
            if(!Constant.isInitHc){
                InitDevice.initHc();
            }
            String deviceIp = getPara("deviceIp");
            //linux 初始化 海康设备
            //Prop use = PropKit.use("config_product.properties");

            //systemload(use.get("path"));
            //devicesInit.run("/usr/tomcat/apache-tomcat-8.5.43/webapps1/WEB-INF/classes");
            HCNetSDKService sendAccessRecord = HCNetSDKService.me;
            int lUserID = sendAccessRecord.initAndLogin(deviceIp);
            //开门，以门 1 为例
            boolean bRet;
            int lGatewayIndex = 1;//门禁序号，从 1 开始，-1 表示对所有门进行操作
            int dwStaic = 1;//命令值：0-关闭，1-打开，2-常开，3-常关
            HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;
            bRet = hcNetSDK.NET_DVR_ControlGateway(lUserID, lGatewayIndex, dwStaic);
            if (!bRet) {
                System.out.println("NET_DVR_ControlGateway failed, error:%d__" + hcNetSDK.NET_DVR_GetLastError());
                hcNetSDK.NET_DVR_Logout(lUserID);
                hcNetSDK.NET_DVR_Cleanup();
                logger.error("开门失败。。");
                renderText("开门失败。。");
            }else{
                //注销用户
                hcNetSDK.NET_DVR_Logout(lUserID);
                //释放 SDK 资源
                hcNetSDK.NET_DVR_Cleanup();
                logger.info("开门成功。。");
                renderText("开门成功。。");
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("开门失败。。");
            renderText("开门失败。。");
        }
    }

    public void reboot(){
        String deviceType = getPara("deviceType");
        String deviceIp = getPara("deviceIp");
        if(deviceType.equals("DS-K5671")){
            HCNetSDKService sendAccessRecord = HCNetSDKService.me;
            int lUserID = sendAccessRecord.initAndLogin(deviceIp);
            HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;
            if(hcNetSDK.NET_DVR_RebootDVR(lUserID)){
                logger.info(deviceType+"设备重启成功！！！");
            }else{
                logger.error(deviceType+"设备重启失败！！！"+hcNetSDK.NET_DVR_GetLastError());
            }
        }else if(deviceType.equals("DH-ASI728")){
            if (LoginModule.netsdk.CLIENT_ControlDevice(LoginModule.m_hLoginHandle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_REBOOT, null, 3000)) {
                logger.info(deviceType+"设备重启成功！！！");
            }else{
                logger.error(deviceType+"设备重启失败！！" + ToolKits.getErrorCodePrint());
            }
        }


        renderNull();
    }
}

