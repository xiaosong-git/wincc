package com.xiaosong.sdkConfig;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.common.DateUtil;
import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.lib.ToolKits;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.xiaosong.common.accessrecord.AccessRecordService;
import com.xiaosong.common.websocket.WebSocket;
import com.xiaosong.common.wincc.buildingserverservice.BuildingServerService;
import com.xiaosong.common.wincc.companyuser.TbCompanyUserService;
import com.xiaosong.common.wincc.device.TbDeviceService;
import com.xiaosong.common.wincc.devicerelated.TbDevicerelatedService;
import com.xiaosong.common.wincc.visitor.VisitorService;
import com.xiaosong.config.InitDevice;
import com.xiaosong.constant.Constants;
import com.xiaosong.model.*;
import com.xiaosong.util.Control24DeviceUtil;
import com.xiaosong.util.NameUtils;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class HCNetSDKService {

    Logger logger = Logger.getLogger(HCNetSDKService.class);
    public static final HCNetSDKService me = new HCNetSDKService();

    public HCNetSDKService() {
    }

    private TbDeviceService tbDeviceService = TbDeviceService.me;

    private TbCompanyUserService tbCompanyUserService = TbCompanyUserService.me;

    private TbDevicerelatedService tbDevicerelatedService = TbDevicerelatedService.me;

    private BuildingServerService serverService = BuildingServerService.me;

    private VisitorService visitorService = VisitorService.me;

    private AccessRecordService accessRecordService = AccessRecordService.me;

    private WebSocket webSocket= new WebSocket();

    HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    int lUserID; // 用户句柄

    NativeLong m_lUploadHandle;

    NativeLong m_UploadStatus;

    FMSGCallBack_V31 fMSFCallBack_V31 = null;
    ExceptionCallBack exceptionCallBack = null;

    public boolean setCardAndFace(String deviceIP, Map<String, String> params) throws IOException {
        // TODO Auto-generated method stub
        if (deviceIP.isEmpty() || params.size() == 0) {
            logger.error("海康门禁下发名单参数错误");
            return false;
        }

        String strCardNo = params.get("strCardNo");    //用户卡号
        int dwEmployeeNo = Integer.valueOf(params.get("userId"));    //用户工号
        String name = params.get("userName");    //用户名字
        logger.info("用户名字：" + name);
        String filePath = params.get("filePath");  //用户照片路径
        String personType = params.get("personType");

        String startTime = "";
        String endTime = "";
        if (!"staff".equals(personType) && !"visitor".equals(personType)) {
            logger.error("人员类型参数错误");
            return false;
        } else if (personType.equals("visitor")) {
            startTime = params.get("startTime");
            endTime = params.get("endTime");
        }
        if (strCardNo.isEmpty() || name.isEmpty() || filePath.isEmpty()) {
            logger.error("海康门禁下发名单map参数错误");
            return false;
        }
        boolean re = false;
        lUserID = initAndLogin(deviceIP);
        if (lUserID < 0) {
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            return false;
        }
        int iErr = 0;
        // 设置卡参数
        HCNetSDK.NET_DVR_CARD_CFG_COND m_struCardInputParamSet = new HCNetSDK.NET_DVR_CARD_CFG_COND();
        m_struCardInputParamSet.read();
        m_struCardInputParamSet.dwSize = m_struCardInputParamSet.size();
        m_struCardInputParamSet.dwCardNum = 1;
        m_struCardInputParamSet.byCheckCardNo = 1;

        Pointer cardInBuffer = m_struCardInputParamSet.getPointer();
        m_struCardInputParamSet.write();

        Pointer cardUserData = null;
        FRemoteCfgCallBackCardSet fRemoteCfgCallBackCardSet = new FRemoteCfgCallBackCardSet();
        int cardHandle = this.hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_CFG_V50, cardInBuffer,
                m_struCardInputParamSet.size(), fRemoteCfgCallBackCardSet, cardUserData);
        if (cardHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("建立长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            return false;
        }

        HCNetSDK.NET_DVR_CARD_CFG_V50 struCardInfo = new HCNetSDK.NET_DVR_CARD_CFG_V50(); // 卡参数
        struCardInfo.read();
        struCardInfo.dwSize = struCardInfo.size();
        struCardInfo.dwModifyParamType = 0x6DAF;// 0x00000001 + 0x00000002 + 0x00000004 + 0x00000008 +
        // 0x00000010 + 0x00000020 + 0x00000080 + 0x00000100 + 0x00000200 + 0x00000400 +
        // 0x00000800;
        /***
         * #define CARD_PARAM_CARD_VALID 0x00000001 //卡是否有效参数 #define CARD_PARAM_VALID
         * 0x00000002 //有效期参数 #define CARD_PARAM_CARD_TYPE 0x00000004 //卡类型参数 #define
         * CARD_PARAM_DOOR_RIGHT 0x00000008 //门权限参数 #define CARD_PARAM_LEADER_CARD
         * 0x00000010 //首卡参数 #define CARD_PARAM_SWIPE_NUM 0x00000020 //最大刷卡次数参数 #define
         * CARD_PARAM_GROUP 0x00000040 //所属群组参数 #define CARD_PARAM_PASSWORD 0x00000080
         * //卡密码参数 #define CARD_PARAM_RIGHT_PLAN 0x00000100 //卡权限计划参数 #define
         * CARD_PARAM_SWIPED_NUM 0x00000200 //已刷卡次数 #define CARD_PARAM_EMPLOYEE_NO
         * 0x00000400 //工号 #define CARD_PARAM_NAME 0x00000800 //姓名
         */
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struCardInfo.byCardNo[i] = 0;
        }
        for (int i = 0; i < strCardNo.length(); i++) {
            struCardInfo.byCardNo[i] = strCardNo.getBytes()[i];
        }

        struCardInfo.byCardValid = 1;
        struCardInfo.byCardType = 1;
        struCardInfo.byLeaderCard = 0;
        struCardInfo.byDoorRight[0] = 1; // 门1有权限
        struCardInfo.wCardRightPlan[0].wRightPlan[0] = 1; // 门1关联卡参数计划模板1


        // 卡有效期,员工无期限，访客限制时间
        if ("visitor".equals(personType)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                Calendar startCalendar = Calendar.getInstance();
                Calendar endCalendar = Calendar.getInstance();
                startCalendar.setTime(startDate);
                endCalendar.setTime(endDate);

                struCardInfo.struValid.byEnable = 1;
                struCardInfo.struValid.struBeginTime.wYear = (short) startCalendar.get(Calendar.YEAR);
                struCardInfo.struValid.struBeginTime.byMonth = (byte) (startCalendar.get(Calendar.MONTH) + 1);
                struCardInfo.struValid.struBeginTime.byDay = (byte) startCalendar.get(Calendar.DAY_OF_MONTH);
                struCardInfo.struValid.struBeginTime.byHour = (byte) startCalendar.get(Calendar.HOUR_OF_DAY);
                struCardInfo.struValid.struBeginTime.byMinute = (byte) startCalendar.get(Calendar.MINUTE);
                struCardInfo.struValid.struBeginTime.bySecond = 0;
                struCardInfo.struValid.struEndTime.wYear = (short) endCalendar.get(Calendar.YEAR);
                struCardInfo.struValid.struEndTime.byMonth = (byte) (endCalendar.get(Calendar.MONTH) + 1);
                struCardInfo.struValid.struEndTime.byDay = (byte) endCalendar.get(Calendar.DAY_OF_MONTH);
                struCardInfo.struValid.struEndTime.byHour = (byte) endCalendar.get(Calendar.HOUR_OF_DAY);
                struCardInfo.struValid.struEndTime.byMinute = (byte) endCalendar.get(Calendar.MINUTE);
                struCardInfo.struValid.struEndTime.bySecond = 0;
                logger.info(struCardInfo.struValid.struEndTime.wYear + "*****" + struCardInfo.struValid.struEndTime.byMonth + "**" + struCardInfo.struValid.struEndTime.byDay);
                logger.info(struCardInfo.struValid.struEndTime.byHour + "**" + struCardInfo.struValid.struEndTime.byMinute);
            } catch (ParseException e2) {
                hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        } else {
            struCardInfo.struValid.byEnable = 1;
            struCardInfo.struValid.struBeginTime.wYear = 2010;
            struCardInfo.struValid.struBeginTime.byMonth = 12;
            struCardInfo.struValid.struBeginTime.byDay = 1;
            struCardInfo.struValid.struBeginTime.byHour = 0;
            struCardInfo.struValid.struBeginTime.byMinute = 0;
            struCardInfo.struValid.struBeginTime.bySecond = 0;
            struCardInfo.struValid.struEndTime.wYear = 2024;
            struCardInfo.struValid.struEndTime.byMonth = 12;
            struCardInfo.struValid.struEndTime.byDay = 1;
            struCardInfo.struValid.struEndTime.byHour = 0;
            struCardInfo.struValid.struEndTime.byMinute = 0;
            struCardInfo.struValid.struEndTime.bySecond = 0;
        }


        struCardInfo.dwMaxSwipeTime = 0; // 无次数限制
        struCardInfo.dwSwipeTime = 0;
        struCardInfo.byCardPassword = "123456".getBytes();
        struCardInfo.dwEmployeeNo = dwEmployeeNo;
        struCardInfo.wSchedulePlanNo = 1;
        struCardInfo.bySchedulePlanType = 2;
        struCardInfo.wDepartmentNo = 1;

        try {
            byte[] strCardName = name.getBytes("GBK");
            for (int i = 0; i < HCNetSDK.NAME_LEN; i++) {
                struCardInfo.byName[i] = 0;
            }
            for (int i = 0; i < strCardName.length; i++) {
                struCardInfo.byName[i] = strCardName[i];
            }
        } catch (UnsupportedEncodingException e) {
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        struCardInfo.write();
        Pointer cardSendBufSet = struCardInfo.getPointer();

        if (!hCNetSDK.NET_DVR_SendRemoteConfig(cardHandle, 0x3, cardSendBufSet, struCardInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        try {
            new Thread().sleep(1500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (!hCNetSDK.NET_DVR_StopRemoteConfig(cardHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            return false;
        }

        // 设置人脸参数
        HCNetSDK.NET_DVR_FACE_PARAM_COND m_struFaceSetParam = new HCNetSDK.NET_DVR_FACE_PARAM_COND();
        m_struFaceSetParam.dwSize = m_struFaceSetParam.size();

        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            m_struFaceSetParam.byCardNo[i] = 0;
        }
        System.arraycopy(strCardNo.getBytes(), 0, m_struFaceSetParam.byCardNo, 0, strCardNo.length());

        m_struFaceSetParam.byEnableCardReader[0] = 1;
        m_struFaceSetParam.dwFaceNum = 1;
        m_struFaceSetParam.byFaceID = 1;
        m_struFaceSetParam.write();

        Pointer faceInBuffer = m_struFaceSetParam.getPointer();

        Pointer faceUserData = null;
        FRemoteCfgCallBackFaceSet fRemoteCfgCallBackFaceSet = new FRemoteCfgCallBackFaceSet();

        int lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE_PARAM_CFG, faceInBuffer,
                m_struFaceSetParam.size(), fRemoteCfgCallBackFaceSet, faceUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("建立长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        HCNetSDK.NET_DVR_FACE_PARAM_CFG struFaceInfo = new HCNetSDK.NET_DVR_FACE_PARAM_CFG(); // 人脸参数
        struFaceInfo.read();
        struFaceInfo.dwSize = struFaceInfo.size();


        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struFaceInfo.byCardNo[i] = 0;
        }
        System.arraycopy(strCardNo.getBytes(), 0, struFaceInfo.byCardNo, 0, strCardNo.length());

        struFaceInfo.byEnableCardReader[0] = 1; // 需要下发人脸的读卡器，按数组表示，每位数组表示一个读卡器，数组取值：0-不下发该读卡器，1-下发到该读卡器
        struFaceInfo.byFaceID = 1; // 人脸ID编号，有效取值范围：1~2
        struFaceInfo.byFaceDataType = 1; // 人脸数据类型：0- 模板（默认），1- 图片

        /*****************************************
         * 从本地文件里面读取JPEG图片二进制数据
         *****************************************/
        FileInputStream picfile = null;
        int picdataLength = 0;
        try {
            //	String filePath = Constants.StaffPath + "/" + companyUser.getUserName() + companyUser.getCompanyId()
            //			+ ".jpg";
            System.out.println("照片路径："+filePath);
            File picture = new File(filePath);
            if (!picture.exists()) {
                hCNetSDK.NET_DVR_Logout(lUserID);
                logger.error("照片路径不存在"+name);
                return false;
            }
            picfile = new FileInputStream(picture);
            picdataLength = picfile.available();
            if (picdataLength < 0) {
                logger.error("input file dataSize < 0");
                hCNetSDK.NET_DVR_Logout(lUserID);
                picfile.close();
                return false;
            }
            HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
            picfile.read(ptrpicByte.byValue);
            ptrpicByte.write();
            struFaceInfo.pFaceBuffer = ptrpicByte.getPointer();
        } catch (FileNotFoundException e) {
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            e.printStackTrace();
        } catch (IOException e1) {
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            e1.printStackTrace();
        }
        struFaceInfo.dwFaceLen = picdataLength;
        struFaceInfo.write();
        Pointer pSendBufSet = struFaceInfo.getPointer();
        if (!hCNetSDK.NET_DVR_SendRemoteConfig(lHandle, 0x9, pSendBufSet, struFaceInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("NET_DVR_SendRemoteConfig失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            picfile.close();
            return false;
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (fRemoteCfgCallBackFaceSet.isSend != 1 || fRemoteCfgCallBackFaceSet.isStatus != 1) {
            logger.error("下发人脸回调函数isStatus=-1或者isSend=-1");
        }
        if (!hCNetSDK.NET_DVR_StopRemoteConfig(lHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            picfile.close();
            return false;
        }

        picfile.close();
        re = hCNetSDK.NET_DVR_Logout(lUserID);
        if (re) {
            logger.info("卡号及人脸下发成功，注销成功");
        } else {
            logger.error("卡号及人脸下发成功，注销失败");
        }
        return true;
    }

    public boolean delFace(String deviceIP,String idCardNo) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        int dellogin = initAndLogin(deviceIP);
        if(dellogin<0) {
            logger.error("删除人脸登录失败");
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        int iErr = 0;
        // 删除人脸数据
        HCNetSDK.NET_DVR_FACE_PARAM_CTRL m_struFaceDel = new HCNetSDK.NET_DVR_FACE_PARAM_CTRL();
        m_struFaceDel.dwSize = m_struFaceDel.size();
        m_struFaceDel.byMode = 0; // 删除方式：0- 按卡号方式删除，1- 按读卡器删除

        m_struFaceDel.struProcessMode.setType(HCNetSDK.NET_DVR_FACE_PARAM_BYCARD.class);
        m_struFaceDel.struProcessMode.struByCard.byCardNo = idCardNo.getBytes();// 需要删除人脸关联的卡号
        m_struFaceDel.struProcessMode.struByCard.byEnableCardReader[0] = 1; // 读卡器
        m_struFaceDel.struProcessMode.struByCard.byFaceID[0] = 1; // 人脸ID
        m_struFaceDel.write();

        Pointer lpInBuffer = m_struFaceDel.getPointer();

        boolean lRemoteCtrl = hCNetSDK.NET_DVR_RemoteControl(lUserID, HCNetSDK.NET_DVR_DEL_FACE_PARAM_CFG, lpInBuffer,
                m_struFaceDel.size());
        if (!lRemoteCtrl) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("删除人脸图片失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if(logout) {
                logger.info("注销成功");
            }else {
                logger.error("注销失败");
            }
            return false;
        } else {
            logger.info("删除人脸图片成功!");
            hCNetSDK.NET_DVR_Logout(lUserID);
            return true;
        }
    }

    public boolean delCard(String deviceIP,Map<String,String> params)
            throws UnsupportedEncodingException {
        // TODO Auto-generated method stub

        if(deviceIP.isEmpty() ||  params.size()==0) {
            logger.error("海康门禁删除名单参数错误");
            return false;
        }
        lUserID = initAndLogin(deviceIP);
        if (lUserID < 0) {
            return false;
        }
        String strCardNo = params.get("strCardNo");	//用户卡号
        int dwEmployeeNo = Integer.valueOf(params.get("userId"));	//用户工号
        String name = params.get("userName");	//用户名字
        if(strCardNo.isEmpty()||name.isEmpty()) {
            logger.error("海康门禁删除名单map参数错误");
            return false;
        }
        int iErr = 0;

        // 设置卡参数
        HCNetSDK.NET_DVR_CARD_CFG_COND m_struCardInputParamSet = new HCNetSDK.NET_DVR_CARD_CFG_COND();
        m_struCardInputParamSet.read();
        m_struCardInputParamSet.dwSize = m_struCardInputParamSet.size();
        m_struCardInputParamSet.dwCardNum = 1;
        m_struCardInputParamSet.byCheckCardNo = 1;

        Pointer lpInBuffer = m_struCardInputParamSet.getPointer();
        m_struCardInputParamSet.write();

        Pointer pUserData = null;
        FRemoteCfgCallBackCardSet fRemoteCfgCallBackCardSet = new FRemoteCfgCallBackCardSet();

        int lHandle = this.hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_CFG_V50, lpInBuffer,
                m_struCardInputParamSet.size(), fRemoteCfgCallBackCardSet, pUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("建立长连接失败，错误号：" + iErr);

            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                logger.info("下发卡后注销成功");
            } else {
                logger.error("下发卡后注销失败");
            }
            return false;
        }

        HCNetSDK.NET_DVR_CARD_CFG_V50 struCardInfo = new HCNetSDK.NET_DVR_CARD_CFG_V50(); // 卡参数
        struCardInfo.read();
        struCardInfo.dwSize = struCardInfo.size();
        struCardInfo.dwModifyParamType = 0x6DAF;// 0x00000001 + 0x00000002 + 0x00000004 + 0x00000008 +
        // 0x00000010 + 0x00000020 + 0x00000080 + 0x00000100 + 0x00000200 + 0x00000400 +
        // 0x00000800;
        /***
         * #define CARD_PARAM_CARD_VALID 0x00000001 //卡是否有效参数 #define CARD_PARAM_VALID
         * 0x00000002 //有效期参数 #define CARD_PARAM_CARD_TYPE 0x00000004 //卡类型参数 #define
         * CARD_PARAM_DOOR_RIGHT 0x00000008 //门权限参数 #define CARD_PARAM_LEADER_CARD
         * 0x00000010 //首卡参数 #define CARD_PARAM_SWIPE_NUM 0x00000020 //最大刷卡次数参数 #define
         * CARD_PARAM_GROUP 0x00000040 //所属群组参数 #define CARD_PARAM_PASSWORD 0x00000080
         * //卡密码参数 #define CARD_PARAM_RIGHT_PLAN 0x00000100 //卡权限计划参数 #define
         * CARD_PARAM_SWIPED_NUM 0x00000200 //已刷卡次数 #define CARD_PARAM_EMPLOYEE_NO
         * 0x00000400 //工号 #define CARD_PARAM_NAME 0x00000800 //姓名
         */
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struCardInfo.byCardNo[i] = 0;
        }
        for (int i = 0; i < strCardNo.length(); i++) {
            struCardInfo.byCardNo[i] = strCardNo.getBytes()[i];
        }

        struCardInfo.byCardValid = 0;// 0-无效,1-有效

        struCardInfo.byCardType = 1;
        struCardInfo.byLeaderCard = 0;
        struCardInfo.byDoorRight[0] = 1; // 门1有权限
        struCardInfo.wCardRightPlan[0].wRightPlan[0] = 1; // 门1关联卡参数计划模板1

        // 卡有效期
        struCardInfo.struValid.byEnable = 1;
        struCardInfo.struValid.struBeginTime.wYear = 2010;
        struCardInfo.struValid.struBeginTime.byMonth = 12;
        struCardInfo.struValid.struBeginTime.byDay = 1;
        struCardInfo.struValid.struBeginTime.byHour = 0;
        struCardInfo.struValid.struBeginTime.byMinute = 0;
        struCardInfo.struValid.struBeginTime.bySecond = 0;
        struCardInfo.struValid.struEndTime.wYear = 2024;
        struCardInfo.struValid.struEndTime.byMonth = 12;
        struCardInfo.struValid.struEndTime.byDay = 1;
        struCardInfo.struValid.struEndTime.byHour = 0;
        struCardInfo.struValid.struEndTime.byMinute = 0;
        struCardInfo.struValid.struEndTime.bySecond = 0;

        struCardInfo.dwMaxSwipeTime = 0; // 无次数限制
        struCardInfo.dwSwipeTime = 0;
        struCardInfo.byCardPassword = "123456".getBytes();
        struCardInfo.dwEmployeeNo = dwEmployeeNo;
        struCardInfo.wSchedulePlanNo = 1;
        struCardInfo.bySchedulePlanType = 2;
        struCardInfo.wDepartmentNo = 1;

        byte[] strCardName = name.getBytes("GBK");
        for (int i = 0; i < HCNetSDK.NAME_LEN; i++) {
            struCardInfo.byName[i] = 0;
        }
        for (int i = 0; i < strCardName.length; i++) {
            struCardInfo.byName[i] = strCardName[i];
        }

        struCardInfo.write();
        Pointer pSendBufSet = struCardInfo.getPointer();

        if (!hCNetSDK.NET_DVR_SendRemoteConfig(lHandle, 0x3, pSendBufSet, struCardInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!hCNetSDK.NET_DVR_StopRemoteConfig(lHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            logger.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        logger.info("断开长连接成功!");
        boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
        if (logout) {
            logger.info("下发卡后注销成功");
        } else {
            logger.error("下发卡后注销失败");
        }
        return true;
    }

    public void sendAccessRecord(String deviceIP) {
        // TODO Auto-generated method stub
        exceptionCallBack = new ExceptionCallBack();
        Pointer userCallBack = null;
        int lAlarmHandle; // 布防句柄
        lUserID = initAndLogin(deviceIP);
        hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, lUserID, exceptionCallBack, userCallBack);

        if (fMSFCallBack_V31 == null) {
            fMSFCallBack_V31 = new FMSGCallBack_V31();
            Pointer pUser = null;
            if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                logger.error("设置回调函数失败!");
            }
        }

        HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
        m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
        m_strAlarmInfo.byLevel = 1;
        m_strAlarmInfo.byAlarmInfoType = 1;
        m_strAlarmInfo.byDeployType = 1;
        m_strAlarmInfo.write();

        lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);

        hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, lAlarmHandle, exceptionCallBack, userCallBack);
        if (lAlarmHandle == -1) {
            logger.error("布防失败,失败码：" + hCNetSDK.NET_DVR_GetLastError());
        } else {
            logger.info("布防成功");
        }

    }

    public int initAndLogin(String sDeviceIP) {
        // TODO Auto-generated method stub
        String password = Constants.deviceLoginPassWord;
        String admin = Constants.deviceLoginName;
        HCNetSDK.NET_DVR_USER_LOGIN_INFO struLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();
        HCNetSDK.NET_DVR_DEVICEINFO_V40 struDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();
        int iPort = 8000;
        for (int i = 0; i < sDeviceIP.length(); i++) {
            struLoginInfo.sDeviceAddress[i] = (byte) sDeviceIP.charAt(i);
        }
        for (int i = 0; i < password.length(); i++) {
            struLoginInfo.sPassword[i] = (byte) password.charAt(i);
        }
        for (int i = 0; i < admin.length(); i++) {
            struLoginInfo.sUserName[i] = (byte) admin.charAt(i);
        }
        struLoginInfo.wPort = (short) iPort;
        struLoginInfo.write();
        Pointer struDeviceInfo1 = struDeviceInfo.getPointer();
        Pointer struLoginInfo1 = struLoginInfo.getPointer();
        lUserID = hCNetSDK.NET_DVR_Login_V40(struLoginInfo1, struDeviceInfo1);
        if (lUserID < 0) {
            logger.error("注册失败，失败号：" + hCNetSDK.NET_DVR_GetLastError());
            if(hCNetSDK.NET_DVR_GetLastError()==3){
                logger.error("海康sdk未初始化,开始初始化海康sdk...");
                InitDevice.initHc();
                initAndLogin(sDeviceIP);
            }
        } else {
            logger.info("登录成功");
        }
        //struLoginInfo.clear();  //释放
        //struDeviceInfo.clear(); //释放
        //struDeviceInfo1.clear(struDeviceInfo.size());    //释放
        //struDeviceInfo1.clear(struLoginInfo.size());     //释放
        return lUserID;
    }


    public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
        public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
                              Pointer pUser) {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }

    }

    public void AlarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
                                Pointer pUser) {
        // TODO Auto-generated method stub
        String[] newRow = new String[3];
        // 报警时间
        Date today = new Date();
        // 时间格式
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String sAlarmType = "lCommand=" + lCommand;

        String[] sIP = new String[2];
        // 门禁报警
        // 门禁设备比对成功实现开门及记录通行记录
        if (lCommand == HCNetSDK.COMM_ISAPI_ALARM) {
            logger.info("=====================");
            HCNetSDK.NET_DVR_ALARM_ISAPI_INFO struEventISAPI = new HCNetSDK.NET_DVR_ALARM_ISAPI_INFO();
            struEventISAPI.write();
            Pointer pEventISAPI = struEventISAPI.getPointer();
            pEventISAPI.write(0, pAlarmInfo.getByteArray(0, struEventISAPI.size()), 0, struEventISAPI.size());
            struEventISAPI.read();

            sAlarmType = sAlarmType + "：ISAPI协议报警信息, 数据格式:" + struEventISAPI.byDataType +
                    ", 图片个数:" + struEventISAPI.byPicturesNumber;

            newRow[0] = dateFormat.format(today);
            //报警类型
            newRow[1] = sAlarmType;
            //报警设备IP地址
            sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
            newRow[2] = sIP[0];

            SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String curTime = sf1.format(new Date());
            try {
                ByteBuffer jsonbuffers = struEventISAPI.pAlarmData.getByteBuffer(0, struEventISAPI.dwAlarmDataLen);

                byte[] jsonbytes = new byte[struEventISAPI.dwAlarmDataLen];
                jsonbuffers.rewind();
                jsonbuffers.get(jsonbytes);
                //logger.info(new String(jsonbytes));
                //logger.info("____________"+new String(jsonbytes));
                JSONObject parse = JSON.parseObject(new String(jsonbytes));
                JSONArray jsonArray = JSON.parseArray(parse.getString("CaptureResult"));
                JSONArray jsonArray1 = JSON.parseArray(jsonArray.getJSONObject(0).getString("FaceContrastResult"));
                if (jsonArray1 == null) {
                    return;
                }
                JSONArray jsonArray3 = JSON.parseArray(jsonArray1.getJSONObject(0).getString("faces"));
                if (jsonArray3 == null) {
                    return;
                }
                JSONArray jsonArray4 = JSON.parseArray(jsonArray3.getJSONObject(0).getString("identify"));
                if (jsonArray4 == null) {
                    return;
                }
                JSONArray jsonArray5 = JSON.parseArray(jsonArray4.getJSONObject(0).getString("candidate"));
                if (jsonArray5 == null) {
                    return;
                }
                String reserve_field = jsonArray5.getJSONObject(0).getString("reserve_field");
                String name = new String(JSONObject.parseObject(reserve_field).getString("name").getBytes(), "utf-8");
                String id = (String) jsonArray5.getJSONObject(0).get("human_id");
                logger.info("姓名:" + name + ",picId" + id + ",通行时间" + curTime + ",设备ip" + sIP[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (lCommand == HCNetSDK.COMM_ALARM_ACS) {

            HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
            strACSInfo.write();
            Pointer pACSInfo = strACSInfo.getPointer();
            pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
            strACSInfo.read();

            String idCardNo = new String(strACSInfo.struAcsEventInfo.byCardNo).trim();
            if (idCardNo != null && !"".equals(idCardNo)) {

                int userId = Integer.valueOf(idCardNo.substring(1));
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);


                //抓拍时间解析
                HCNetSDK.NET_DVR_TIME dateTime = strACSInfo.struTime;
                String year = String.valueOf(dateTime.dwYear);
                String month = String.valueOf(dateTime.dwMonth);
                String day = String.valueOf(dateTime.dwDay);
                if (dateTime.dwDay < 10) {
                    day = "0" + dateTime.dwDay;
                }
                if (dateTime.dwMonth < 10) {
                    month = "0" + dateTime.dwMonth;
                }
                String date = year + "-" + month + "-" + day;
                String hour = String.valueOf(dateTime.dwHour);
                String minute = String.valueOf(dateTime.dwMinute);
                String second = String.valueOf(dateTime.dwSecond);
                if (dateTime.dwHour < 10) {
                    hour = "0" + dateTime.dwHour;
                }
                if (dateTime.dwMinute < 10) {
                    minute = "0" + dateTime.dwMinute;
                }
                if (dateTime.dwSecond < 10) {
                    second = "0" + dateTime.dwSecond;
                }
                String time = hour + ":" + minute + ":" + second;

                TbDevicerelated tbDevicerelated = tbDevicerelatedService.findByFaceIP(sIP[0]);
                String username = null;
                String date1 =  String.valueOf(System.currentTimeMillis());

                if ("S".equals(idCardNo.substring(0, 1))) {
                    //员工通行
                    TbCompanyuser user = tbCompanyUserService.findByUserId(userId);
                    String cardNO = "S" + user.getUserId();
                    username = user.getUserName();

                    //redis锁定某人，3秒内只能开启一次
                    Cache redisUtil = Redis.use("xiaosong");
                    String key = "s_" + user.getUserName() + "_" + user.getIdNO();
                    if (redisUtil.get(key) == null) {
                        redisUtil.set(key, "locked");
                        redisUtil.expire(key, 3);
                        //继电器控制
                        //Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), null);
                        //温度获取
                        float temp = 0.00F;
                        String tempStatus = "";
                        if (strACSInfo.byAcsEventInfoExtendV20 == 1) {
                            HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                            struInfoExtend.write();
                            Pointer pInfoExtend = struInfoExtend.getPointer();
                            pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                            struInfoExtend.read();
                            temp = struInfoExtend.fCurrTemperature;
                            tempStatus = String.valueOf(struInfoExtend.byIsAbnomalTemperature);
//                            struInfoExtend.clear();
                        }
                      saverecord(user.getUserName(), user.getIdNO(), "staff", sIP[0], date, time, cardNO, tbDevicerelated.getTurnOver(), temp, tempStatus,date1);
                    }
                } else if ("V".equals(idCardNo.substring(0, 1))) {
                    //访客通行
                    TbVisitor visitor = visitorService.findVisitorId(idCardNo.substring(1));
                    username = visitor.getVisitorName();

                    String cardNO = "V" + visitor.getVisitId();
                    List<TbVisitor> staffs = visitorService.findByBetweenTime(visitor.getVisitorName(), visitor.getVisitorIdCard(), getDateTime());
                    if (staffs.size() > 0) {
                        String key = "v_" + visitor.getVisitorName() + "_" + visitor.getVisitorIdCard();
                        //redis锁定某人，2秒内只能开启一次
                        Cache redisUtils = Redis.use("xiaosong");
                        if (redisUtils.get(key) == null) {
                            redisUtils.set(key, "locked");
                            redisUtils.expire(key, 3);
                            //继电器控制
//                      Control24DeviceUtil.controlDevice(relayted.getRelayIP(), 8080, relayted.getRelayOUT(), null);
                            float temp = 0.00F;
                            String tempStatus = "";
                            if (strACSInfo.byAcsEventInfoExtendV20 == 1) {
                                HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                                struInfoExtend.write();
                                Pointer pInfoExtend = struInfoExtend.getPointer();
                                pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                                struInfoExtend.read();
                                temp = struInfoExtend.fCurrTemperature;
                                tempStatus = String.valueOf(struInfoExtend.byIsAbnomalTemperature);
//                                struInfoExtend.clear();
                            }
                            saverecord(visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor", sIP[0],
                                    date, time, cardNO, tbDevicerelated.getTurnOver(), temp, tempStatus,date1);

                        }
                    } else {
                        logger.info(visitor.getVisitorName() + "不在有效访问时间");
                        return;
                    }
                }

                String scanTime = year + month + day + hour + minute + second;
                getPic(strACSInfo.pPicData, strACSInfo.dwPicDataLen,username , date1);
            } else {
                String cardNo = new String(strACSInfo.struAcsEventInfo.byCardNo).trim(); //卡号
                sAlarmType = sAlarmType + "：门禁主机报警信息，卡号：" + cardNo
                        + "，卡类型：" + strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型："
                        + strACSInfo.dwMinor;
                if (strACSInfo.byAcsEventInfoExtendV20 == 1) {
                    HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                    struInfoExtend.write();
                    Pointer pInfoExtend = struInfoExtend.getPointer();
                    pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                    struInfoExtend.read();
                    sAlarmType = sAlarmType + ", 温度:" + struInfoExtend.fCurrTemperature + ", 是否异常:" + struInfoExtend.byIsAbnomalTemperature;
                }
                logger.info(sAlarmType);
            }
//            strACSInfo.clear();
        }
    }

    class ExceptionCallBack implements HCNetSDK.FExceptionCallBack {

        @Override
        public void invoke(int dwType, int lUserID, int lHandle, Pointer pUser) {
            // TODO Auto-generated method stub
            switch (dwType) {

                case HCNetSDK.EXCEPTION_ALARMRECONNECT:
                    logger.info("门禁通行长连接尝试重连...");
                    HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
                    m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
                    m_strAlarmInfo.byLevel = 1;
                    m_strAlarmInfo.byAlarmInfoType = 1;
                    m_strAlarmInfo.bySupport = 1;
                    m_strAlarmInfo.byDeployType = 1;
                    m_strAlarmInfo.write();
                    int suc = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
                    if (suc < 0) {
                        logger.error("门禁通行长连接重连失败");

                    } else {
                        logger.info("门禁通行长连接重连成功");
                        hCNetSDK.NET_DVR_CloseAlarmChan_V30(suc);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    class FRemoteCfgCallBackCardSet implements HCNetSDK.FRemoteConfigCallback {

        public int sendFlag = -1; // 卡状态下发返回标记（1成功，-1失败,0正在下发）

        public void invoke(int dwType, Pointer lpBuffer, int dwBufLen, Pointer pUserData) {
            switch (dwType) {
                case 0:// NET_SDK_CALLBACK_TYPE_STATUS
                    HCNetSDK.BYTE_ARRAY struCallbackStatus = new HCNetSDK.BYTE_ARRAY(40);
                    struCallbackStatus.write();
                    Pointer pStatus = struCallbackStatus.getPointer();
                    pStatus.write(0, lpBuffer.getByteArray(0, struCallbackStatus.size()), 0, dwBufLen);
                    struCallbackStatus.read();

                    int iStatus = 0;
                    byte[] byCardNo;
                    for (int i = 0; i < 4; i++) {
                        int ioffset = i * 8;
                        int iByte = struCallbackStatus.byValue[i] & 0xff;
                        iStatus = iStatus + (iByte << ioffset);
                    }

                    switch (iStatus) {
                        case 1000:// NET_SDK_CALLBACK_STATUS_SUCCESS
                            logger.info("下发卡参数成功");
                            sendFlag = 1;
                            break;
                        case 1001:
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 4, byCardNo, 0, 32);
                            logger.info("正在下发人脸参数中,dwStatus:" + iStatus + ",卡号:" + new String(byCardNo).trim());
                            sendFlag = 0;
                            break;
                        case 1002:
                            int iErrorCode = 0;
                            for (int i = 0; i < 4; i++) {
                                int ioffset = i * 8;
                                int iByte = struCallbackStatus.byValue[i + 4] & 0xff;
                                iErrorCode = iErrorCode + (iByte << ioffset);
                            }
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 8, byCardNo, 0, 32);
                            logger.error("下发人脸参数失败, dwStatus:" + iStatus + ",错误号:" + iErrorCode + ",卡号:"
                                    + new String(byCardNo).trim());
                            logger.error("下发卡参数失败,卡号:" + new String(byCardNo).trim() + ",错误号:" + iErrorCode);
                            sendFlag = -1;
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    class FRemoteCfgCallBackFaceSet implements HCNetSDK.FRemoteConfigCallback {

        int isStatus = -1;    //设备状态
        int isSend = -1;    //下发状态

        public void invoke(int dwType, Pointer lpBuffer, int dwBufLen, Pointer pUserData) {
            logger.info("长连接回调获取数据,NET_SDK_CALLBACK_TYPE_STATUS:" + dwType);
            switch (dwType) {
                case 0:// NET_SDK_CALLBACK_TYPE_STATUS
                    HCNetSDK.BYTE_ARRAY struCallbackStatus = new HCNetSDK.BYTE_ARRAY(40);
                    struCallbackStatus.write();
                    Pointer pStatus = struCallbackStatus.getPointer();
                    pStatus.write(0, lpBuffer.getByteArray(0, struCallbackStatus.size()), 0, dwBufLen);
                    struCallbackStatus.read();

                    int iStatus = 0;
                    byte[] byCardNo;

                    for (int i = 0; i < 4; i++) {
                        int ioffset = i * 8;
                        int iByte = struCallbackStatus.byValue[i] & 0xff;
                        iStatus = iStatus + (iByte << ioffset);
                    }

                    switch (iStatus) {
                        case 1000:// NET_SDK_CALLBACK_STATUS_SUCCESS

                            logger.info("下发人脸参数成功,dwStatus:" + iStatus);
                            isSend = 1;
                            break;
                        case 1001:
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 4, byCardNo, 0, 32);
                            logger.info("正在下发人脸参数中,dwStatus:" + iStatus + ",卡号:" + new String(byCardNo).trim());
                            break;
                        case 1002:
                            int iErrorCode = 0;
                            for (int i = 0; i < 4; i++) {
                                int ioffset = i * 8;
                                int iByte = struCallbackStatus.byValue[i + 4] & 0xff;
                                iErrorCode = iErrorCode + (iByte << ioffset);
                            }
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 8, byCardNo, 0, 32);
                            logger.error("下发人脸参数失败, dwStatus:" + iStatus + ",错误号:" + iErrorCode);
                            break;
                    }
                    break;
                case 2:// 获取状态数据
                    HCNetSDK.NET_DVR_FACE_PARAM_STATUS m_struFaceStatus = new HCNetSDK.NET_DVR_FACE_PARAM_STATUS();
                    m_struFaceStatus.write();
                    Pointer pStatusInfo = m_struFaceStatus.getPointer();
                    pStatusInfo.write(0, lpBuffer.getByteArray(0, m_struFaceStatus.size()), 0, m_struFaceStatus.size());
                    m_struFaceStatus.read();
                    logger.info("照片状态：" + m_struFaceStatus.byCardReaderRecvStatus[0]);
                    if (m_struFaceStatus.byCardReaderRecvStatus[0] == 1 || m_struFaceStatus.byCardReaderRecvStatus[0] == 4) {
                        isStatus = 1;
                    }
                default:
                    break;
            }
        }
    }

    /**
     * 获取通行照片
     *
     * @param picData      照片日期
     * @param dwPicDataLen 照片大小
    //     * @param cardNO       身份证号码
    //     * @param dateTime     准确时间
     */
    private void getPic(Pointer picData, int dwPicDataLen,String username,String date1) {
        if (dwPicDataLen > 0) {

            File file = new File(Constants.AccessRecPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            FileOutputStream fout;
            HCNetSDKService UI;
            boolean ss = false;


            try {

//                fout = new FileOutputStream(Constants.AccessRecPath+ "/" + username+"_"+getDateTime() + ".jpg");
                fout = new FileOutputStream(Constants.AccessRecPath+ "/" + username+"_"+date1 + ".jpg");
                logger.info("通行照片路径:"+Constants.AccessRecPath+ "/" + username+"_"+date1 + ".jpg");
                long offset = 0;
                ByteBuffer buffers = picData.getByteBuffer(offset, dwPicDataLen);
                byte[] bytes = new byte[dwPicDataLen];
                buffers.rewind();
                buffers.get(bytes);
                fout.write(bytes);
                fout.close();
                //裁剪
                new HCNetSDKService(Constants.AccessRecPath+"/"+ username+"_"+date1+".jpg", Constants.AccessRecPath, username+"_"+date1,"jpg",180,180);
//                ss = UI.createThumbnail();
//                if (ss) {
//                    System.out.println("Success");
//                } else {
//                    System.out.println("Error");
//                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }

    }

    public static String fromFileStr;
    public static String saveToFileStr;
    public static String sysimgfile;
    public static int width;
    public static int height;
    public static String suffix;
    /**
     * @param fromFileStr
     *            原始图片完整路径
     * @param saveToFileStr
     *            缩略图片保存路径
     *            处理后的图片文件名前缀
     *
     */
    public HCNetSDKService(String fromFileStr, String saveToFileStr, String sysimgfile,String suffix,int width,int height) {
        this.fromFileStr = fromFileStr;
        this.saveToFileStr = saveToFileStr;
        this.sysimgfile = sysimgfile;
        this.width=width;
        this.height=height;
        this.suffix=suffix;
    }
    //裁剪照片
    public boolean createThumbnail() throws Exception {
        // fileExtNmae是图片的格式 gif JPG 或png
        // String fileExtNmae="";
        File F = new File(fromFileStr);
        if (!F.isFile())
            throw new Exception(F
                    + " is not image file error in CreateThumbnail!");
        File ThF = new File(saveToFileStr, sysimgfile +"."+suffix);
        BufferedImage buffer = ImageIO.read(F);
        /*
         * 核心算法，计算图片的压缩比
         */
        int w= buffer.getWidth();
        int h=buffer.getHeight();
        double ratiox = 1.0d;
        double ratioy = 1.0d;

        ratiox= w * ratiox / width;
        ratioy= h * ratioy / height;

        if( ratiox >= 1){
            if(ratioy < 1){
                ratiox = height * 1.0 / h;
            }else{
                if(ratiox > ratioy){
                    ratiox = height * 1.0 / h;
                }else{
                    ratiox = width * 1.0 / w;
                }
            }
        }else{
            if(ratioy < 1){
                if(ratiox > ratioy){
                    ratiox = height * 1.0 / h;
                }else{
                    ratiox = width * 1.0 / w;
                }
            }else{
                ratiox = width * 1.0 / w;
            }
        }
        /*
         * 对于图片的放大或缩小倍数计算完成，ratiox大于1，则表示放大，否则表示缩小
         */
        AffineTransformOp op = new AffineTransformOp(AffineTransform
                .getScaleInstance(ratiox, ratiox), null);
        buffer = op.filter(buffer, null);
        //从放大的图像中心截图
        buffer = buffer.getSubimage((buffer.getWidth()-width)/2, (buffer.getHeight() - height) / 2, width, height);
        try {
            ImageIO.write(buffer, suffix, ThF);
        } catch (Exception ex) {
            throw new Exception(" ImageIo.write error in CreatThum.: "
                    + ex.getMessage());
        }
        return (true);
    }

    /**
     * 保存 通行记录
     *
     * @param name       通行人员名称
     * @param idCard     通行人员身份证号码
     * @param personType 通行人员类型
     * @param faceIP     设备ip
     * @param date       通行日期
     * @param time       通行时间
     * @param cardNO     卡号
     */
    public void saverecord(String name, String idCard, String personType, String faceIP, String date,
                           String time, String cardNO, String out, float Temperature, String Abnomal,String date1) {
        // TODO Auto-generated method stub
        TbAccessrecord accessRecord = new TbAccessrecord();
        accessRecord.setOrgCode(serverService.findByOrgCode());
        accessRecord.setPospCode(serverService.findPospCode());
        accessRecord.setScanDate(date);
        accessRecord.setScanTime(time);
        accessRecord.setDeviceType("FACE");
        accessRecord.setDeviceIp(faceIP);
        accessRecord.setUserType(personType);
        accessRecord.setUserName(name);
        accessRecord.setIdCard(idCard);
        accessRecord.setTurnOver(out);
        accessRecord.setIsSendFlag("F");
        accessRecord.setCardNO(cardNO);
        accessRecord.setTemperature(String.valueOf(Temperature));
        accessRecord.setAbnomal(Abnomal);
        accessRecord.setPhoto("/Recored/"+name+"_"+date1 + ".jpg");
        accessRecord.save();

        AccessRecordService srv = AccessRecordService.me;
        String accDate = getDate();
        //人脸设备
        Integer face = srv.findFace(accDate);
        //二维码设备
        Integer qrCode = srv.findQRCode(accDate);
        //员工通行
        Integer staff = srv.findStaff(accDate);
        //访客通行
        Integer visitor = srv.findVisitor(accDate);
        List<Object> list = new ArrayList<>();
        JSONObject jo = new JSONObject();
        jo.put("scanDate", date+" "+time);
        jo.put("tmp", String.valueOf(Temperature));
        jo.put("deviceIp", faceIP);
        jo.put("userName", NameUtils.AccordingToName(name));
        jo.put("photo", "/Recored/"+name+"_"+date1 + ".jpg");
        jo.put("userType", personType);
        jo.put("deviceType", "FACE");
        jo.put("face", face);
        jo.put("qrCode", qrCode);
        jo.put("staff", staff);
        jo.put("visitor", visitor);
        list.add(jo);
        String str = JSONObject.toJSONString(list);
        webSocket.onMessage(str);

    }

    //获取当前时间的  年-月-日  时:分:秒
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    //获取 当前时间的 年-月-日
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }


    // 订阅句柄
    public NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0);
    //public AnalyzerDataCB analyzerCallback = new AnalyzerDataCB();
    public String ip = null;
    //public static NetSDKLib netsdk 		= NetSDKLib.NETSDK_INSTANCE;

    /**
     * 大华设备长接连
     */
    public void dhSendAccessRecord(String deviceIp) {
         NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0);
         AnalyzerDataCB analyzerCallback = new AnalyzerDataCB();
        //登录
        ip = deviceIp;
        com.dhnetsdk.module.LoginModule.login(ip, 37777, Constants.deviceLoginName,  Constants.deviceLoginPassWord);
        m_hAttachHandle = realLoadPic(analyzerCallback);
        if (m_hAttachHandle.longValue() != 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("长接连失败");
        }
    }

    /**
     * 订阅实时上传智能分析数据
     *
     * @return
     */
    private NetSDKLib.LLong realLoadPic(NetSDKLib.fAnalyzerDataCallBack m_AnalyzerDataCB) {
        NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
        /**
         * 说明：
         * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
         *  下列仅订阅了0通道的智能事件.
         */
        int ChannelId = 0;      //通道号
        int bNeedPicture = 0; // 是否需要图片

        m_hAttachHandle = netsdk.CLIENT_RealLoadPictureEx(com.dhnetsdk.module.LoginModule.m_hLoginHandle, ChannelId, NetSDKLib.EVENT_IVS_ALL,
                bNeedPicture, m_AnalyzerDataCB, null, null);
        //修改AttachHandle 获取 ip
        Db.update("update tb_device set deviceName = "+m_hAttachHandle+" where  deviceIp= ?",ip);
        if (m_hAttachHandle.longValue() != 0) {
            logger.info("CLIENT_RealLoadPictureEx Success  ChannelId : " + ChannelId);
        } else {
            logger.error("CLIENT_RealLoadPictureEx Failed!" + ToolKits.getErrorCodePrint());
        }
        return m_hAttachHandle;
    }


    public class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private BufferedImage gateBufferedImage = null;

        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            File path = new File(Constants.AccessRecPath);
            if (!path.exists()) {
                path.mkdir();
            }

            ///< 门禁事件
            System.out.println(lAnalyzerHandle);
            if (dwAlarmType == NetSDKLib.EVENT_IVS_ACCESS_CTL) {
                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                // 通行记录显示
                EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                if (eventQueue != null) {
                    // 开门状态
                    if (msg.bStatus != 1) {
                        System.out.print("--开门状态:" + com.dhnetsdk.common.Res.string().getFailed());
                        return 1;
                    } else {
                        System.out.print("--开门状态:" + com.dhnetsdk.common.Res.string().getSucceed());
                    }

                    // 时间
                    if (msg.UTC == null || msg.UTC.toString().isEmpty()) {
                        System.out.print("");

                    } else {
                        System.out.print("--时间:" + updateDate(msg.UTC.toString()));
                    }

                    // 开门方式
                    System.out.print("--开门方式:" + com.dhnetsdk.common.Res.string().getOpenMethods(msg.emOpenMethod));

                    // 卡名
                    try {
                        System.out.print("--卡名:" + new String(msg.szCardName, "GBK").trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    // 卡号
                    System.out.print("--卡号:" + new String(msg.szCardNo).trim());

                    //用户ID
                    logger.info("--用户ID:" + new String(msg.szUserID).trim());

                    //保存大华设备的通行记录
                    String date1 =  String.valueOf(System.currentTimeMillis());
                    String dateSplit = updateDate(msg.UTC.toString());
                    String[] split = dateSplit.split("\\s");
                    String cardNO = new String(msg.szCardNo).trim();  //卡号
                    String date = split[0];
                    String time = split[1];
                    TbDevice deviceIp = tbDeviceService.findDeviceIp(lAnalyzerHandle);
                    TbDevicerelated tbDevicerelated = tbDevicerelatedService.findByFaceIP(deviceIp.getDeviceIp());
                    String userId = (new String(msg.szUserID).trim());
                    if("".equals(userId)){
                        return 1;
                    }

                    String name=null;
                    //员工通行记录
                    if ("S".equals(cardNO.substring(0, 1))) {
                        TbCompanyuser user = tbCompanyUserService.findByUserId(Integer.parseInt(userId));
                        name=user.getUserName();
                        saverecord(user.getUserName(), user.getIdNO(), "staff", deviceIp.getDeviceIp(), date,
                                time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"",date1);
                    }
                    //访客通行记录
                    else if ("V".equals(cardNO.substring(0, 1))) {
                        TbVisitor visitor = VisitorService.me.findVisitorId(userId);
                        name=visitor.getVisitorName();
                        saverecord(visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor", deviceIp.getDeviceIp(), date,
                                time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"",date1);
                    }


                    // 保存图片，获取图片缓存
                    String snapPicPath = path + "\\" + name+date1 + ".jpg";  // 保存图片地址
                    byte[] buffer = pBuffer.getByteArray(0, dwBufSize);
                    ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(buffer);

                    try {
                        gateBufferedImage = ImageIO.read(byteArrInputGlobal);
                        if (gateBufferedImage != null) {
                            ImageIO.write(gateBufferedImage, "jpg", new File(snapPicPath));
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }

            return 0;
        }
    }


    public String updateDate(String date) {
        String arrTime = (date).replace("/", "/").replace("/", "/");
        String[] deTime  = arrTime.split(" ");
        String ss = deTime [0];
        String[] s1 = ss.split("/");
        String dd = deTime[1];
        String[] d1 = dd.split(":");
        String newStr = s1[0] + "-" + (s1[1].length() > 1 ? "" : "0") + s1[1] + "-"
                + (s1[2].length() > 1 ? "" : "0") + s1[2];
        String minute= String.valueOf(Integer.valueOf((d1[0].length() > 1 ? "" : "0") + d1[0])+8);
        String Str = minute + ":" + (d1[1].length() > 1 ? "" : "0")
                + d1[1] + ":" + (d1[2].length() > 1 ? "" : "0") + d1[2];
        String times = newStr + " " + Str;
        return times;
    }

}
