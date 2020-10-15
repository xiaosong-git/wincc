package com.xiaosong.config;

import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.module.LoginModule;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.sun.jna.Pointer;
import com.xiaosong.constant.Constant;
import com.xiaosong.sdkConfig.HCNetSDK;
import org.apache.log4j.Logger;

public class InitDevice {


    private static Logger logger = Logger.getLogger(InitDevice.class);

    private static Prop use = PropKit.use("config_product.properties");

    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    //判断是否为 winds系统
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static void initHc() {
        //加载库文件
        String path = use.get("path");
        if (isWindows()) {

            //winds 初始化海康设备
            System.load(path + "\\HCCore.dll");
            System.load(path + "\\SuperRender.dll");
            //System.load(path + "\\AudioRender.dll");
            System.load(path + "\\PlayCtrl.dll");
            System.load(path + "\\zlib1.dll");
            System.load(path + "\\hpr.dll");
            System.load(path + "\\ssleay32.dll");
            System.load(path + "\\libeay32.dll");
            System.load(path + "\\hpr.dll");
            System.load(path + "\\hlog.dll");
            //System.load(path + "\\HCNetSDK.dll");
            //海康设备就初始化海康SDK
            boolean initSuc = hCNetSDK.NET_DVR_Init();

            if (!initSuc) {
                logger.warn("海康SDK初始化失败..");
            } else {
                Constant.isInitHc = true;
                logger.warn("海康SDK初始化成功...");
            }
            hCNetSDK.NET_DVR_SetLogToFile(3, "/WEB-INF/sdklog/", false);
        } else {

            //linux 初始化 海康设备
            // TODO Auto-generated method stub
            //设备选型是否为海康设备
            systemload(path);
            boolean init = hCNetSDK.NET_DVR_Init();
            if (init) {
                Constant.isInitHc = true;
                logger.warn("海康SDK初始化成功");
            } else {
                logger.warn("海康SDK初始化失败");
            }
            hCNetSDK.NET_DVR_SetLogToFile(3, "/WEB-INF/sdklog/", false);
        }
    }

    /**
     * 海康SKD加载方式
     */
    public static void systemload(String path) {
        HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

        System.load(path + "/lib/libHCCore.so");
        System.load(path + "/lib/libhpr.so");
        System.load(path + "/lib/libhcnetsdk.so");

        String strPathCom2 = path + "/lib";
        HCNetSDK.NET_DVR_LOCAL_SDK_PATH struComPath2 = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
        System.arraycopy(strPathCom2.getBytes(), 0, struComPath2.sPath, 0, strPathCom2.length());
        struComPath2.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(2, struComPath2.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto = path + "/lib/libssl.so";
        System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
        ptrByteArrayCrypto.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto2 = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto2 = path + "/lib/libcrypto.so.1.0.0";
        System.arraycopy(strPathCrypto2.getBytes(), 0, ptrByteArrayCrypto2.byValue, 0, strPathCrypto2.length());
        ptrByteArrayCrypto2.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto2.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto3 = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto3 = path + "/lib/libcrypto.so";
        System.arraycopy(strPathCrypto3.getBytes(), 0, ptrByteArrayCrypto3.byValue, 0, strPathCrypto3.length());
        ptrByteArrayCrypto3.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto3.getPointer());

    }



    // 网络连接恢复
    private static HaveReConnect haveReConnect = new HaveReConnect();
    // 设备断线通知回调
    private static DisConnect disConnect = new DisConnect();

    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    private static class DisConnect implements NetSDKLib.fDisConnect {
        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            logger.error("ReConnect Device[%s] Port[%d]\n" + "," + pchDVRIP + "," + nDVRPort);
            // 断线提示
            logger.error("人脸识别服务器断开");
        }
    }

    // 网络连接恢复，设备重连成功回调
    // 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    private static class HaveReConnect implements NetSDKLib.fHaveReConnect {
        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            logger.info("ReConnect Device[%s] Port[%d]\n" + "," + pchDVRIP + "," + nDVRPort);

            // 重连提示
            logger.info("人脸识别服务器已连接");
        }
    }

    /**
     * 大华初始化
     */
    public static void initDh() {
        //初始化设备
//        NetSDKLib dhSdk = NetSDKLib.NETSDK_INSTANCE;
//        isInit=dhSdk.CLIENT_Init(disConnect,null);
        boolean init = LoginModule.init(disConnect, haveReConnect);
        if (init) {
            Constant.isInitDh=true;
            logger.info("大华SDK初始化成功!");
        } else {
            logger.error("大华SDK初始化失败!");
        }
    }

}
