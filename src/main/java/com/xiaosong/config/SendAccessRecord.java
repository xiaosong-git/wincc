package com.xiaosong.config;

import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.common.DateUtil;
import com.dhnetsdk.common.Res;
import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.lib.NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO;
import com.dhnetsdk.lib.NetSDKLib.LLong;
import com.dhnetsdk.lib.ToolKits;
import com.dhnetsdk.module.LoginModule;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.xiaosong.common.accessrecord.AccessRecordService;
import com.xiaosong.common.device.DeviceRelatedService;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.websocket.WebSocket;
import com.xiaosong.constant.Constants;
import com.xiaosong.model.*;
import com.xiaosong.sdkConfig.HCNetSDK;
import com.xiaosong.util.NameUtils;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SendAccessRecord {
    private DeviceService srv = DeviceService.me;
    DeviceRelatedService srvDr = DeviceRelatedService.me; //设备关联业务层
    public static int longStatus = 0;
    private static Logger log = Logger.getLogger(SendAccessRecord.class);
    int lAlarmHandle;
    int lUserID;// 用户句柄
    HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    FMSGCallBack_V31 fMSFCallBack_V31 = null;
    ExceptionCallBack exceptionCallBack = null;
    FRemoteCfgCallBackFaceSet fRemoteCfgCallBackFaceSet = null;
    FRemoteCfgCallBackCardSet fRemoteCfgCallBackCardSet = null;
    Cache redisUtils = Redis.use("xiaosong");
    private WebSocket webSocket= new WebSocket();
    NativeLong m_lUploadHandle;

    NativeLong m_UploadStatus;

    /**
     * 初始化并登录设备
     *
     * @param sDeviceIP 设备ip
     * @param admin     设备用户名
     * @param password  设备密码
     * @return
     */
    public int initAndLogin(String sDeviceIP, String admin, String password) {
        // TODO Auto-generated method stub
        HCNetSDK.NET_DVR_USER_LOGIN_INFO struLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();
        HCNetSDK.NET_DVR_DEVICEINFO_V40 struDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();
        Pointer PointerstruDeviceInfoV40 = struDeviceInfo.getPointer();
        Pointer PointerstruLoginInfo = struLoginInfo.getPointer();
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
        lUserID = hCNetSDK.NET_DVR_Login_V40(PointerstruLoginInfo, PointerstruDeviceInfoV40);
        if (lUserID < 0) {
            log.error("注册失败，失败号：" + hCNetSDK.NET_DVR_GetLastError());
            if(hCNetSDK.NET_DVR_GetLastError()==3){
                log.error("海康sdk未初始化,开始初始化海康sdk...");
                InitDevice.initHc();
                initAndLogin(sDeviceIP,admin,password);
            }
        } else {
            log.info("登录成功");
        }

        return lUserID;
    }

    public void length() {
        if (fMSFCallBack_V31 == null) {
            fMSFCallBack_V31 = new FMSGCallBack_V31();
            Pointer pUser = null;
            if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                log.error("设置回调函数失败!");
            }
        }
    }

    /**
     * 门禁设备通行记录
     *
     * @param deviceIP 设备ip
     */
    public void sendAccessRecord(String deviceIP) {
        // TODO Auto-generated method stub
        lUserID = initAndLogin(deviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord);

        if (fMSFCallBack_V31 == null) {
            fMSFCallBack_V31 = new FMSGCallBack_V31();
            Pointer pUser = null;
            if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                log.error("设置回调函数失败!");
            }
        }

        HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
        m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
        m_strAlarmInfo.byLevel = 1;
        m_strAlarmInfo.byAlarmInfoType = 1;
        m_strAlarmInfo.byDeployType = 1;
        m_strAlarmInfo.write();
        System.out.println("--布防类型：0-客户端布防，1-实时布防 ---  :  " + m_strAlarmInfo.byDeployType);

        lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
        exceptionCallBack = new ExceptionCallBack();
        Pointer pUser = null;

        //长连接异常
        hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, lAlarmHandle, exceptionCallBack, pUser);

        if (lAlarmHandle == -1) {
            log.error("布防失败,失败号:"+hCNetSDK.NET_DVR_GetLastError());
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
        } else {
            log.info("布防成功");
            longStatus = 0;
        }
    }

    /**
     * 删除海康设备的 人脸信息
     *
     * @param deviceIP 设备ip
     * @param idCardNo 卡号
     * @return
     * @throws UnsupportedEncodingException
     */
    public boolean delFace(String deviceIP, String idCardNo) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        int dellogin = initAndLogin(deviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord);
        if (dellogin < 0) {
            log.error("删除人脸登录失败");
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
            log.error("删除人脸图片失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        } else {
            log.info("删除人脸图片成功!");
            return true;
        }
    }

    /**
     * 删除海康 卡信息记录
     *
     * @param deviceIP    设备ip
     * @param companyUser 用户
     * @param visitor     访客
     * @return
     * @throws UnsupportedEncodingException
     */
    public boolean delCard(String deviceIP, TbCompanyuser companyUser, TbVisitor visitor)
            throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        lUserID = initAndLogin(deviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord);
        if (lUserID < 0) {
            return false;
        }
        String strCardNo;
        int dwEmployeeNo;
        String name;
        if (null == companyUser) {
            strCardNo = "V" + visitor.getUserId();
            dwEmployeeNo = Integer.valueOf(visitor.getUserId());
            name = visitor.getByVisitorName();
        } else {
            strCardNo = "S" + companyUser.getUserId();
            dwEmployeeNo = companyUser.getUserId();
            name = companyUser.getUserName();
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
            log.error("建立长连接失败，错误号：" + iErr);

            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("下发卡后注销成功");
            } else {
                log.error("下发卡后注销失败");
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
            log.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!hCNetSDK.NET_DVR_StopRemoteConfig(lHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        log.info("断开长连接成功!");
        boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
        if (logout) {
            log.info("注销成功");
        } else {
            log.error("注销失败");
        }
        return true;
    }

//    /**
//     * 定时获取网络摄像头通行记录
//     *
//     * @param deviceIP 设备ip
//     * @param dayInfo  时间信息
//     * @return
//     */
//    public boolean getIPCRecord(String deviceIP, String dayInfo) {
//        // TODO Auto-generated method stub
//        if (null == deviceIP) {
//            log.error("通行记录保存失败，缺少设备信息");
//            return false;
//        }
//        try {
//            int pos = 1;
//            boolean isQuit = false;
//            String id = "1";
//            while (!isQuit) {
//                HCNetSDK.NET_DVR_XML_CONFIG_INPUT struInput = new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
//                struInput.dwSize = struInput.size();
//
//                String str = "POST /ISAPI/Intelligent/FDLib/FCSearch\r\n";
//                HCNetSDK.BYTE_ARRAY ptrUrl = new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
//                System.arraycopy(str.getBytes(), 0, ptrUrl.byValue, 0, str.length());
//                ptrUrl.write();
//                struInput.lpRequestUrl = ptrUrl.getPointer();
//                struInput.dwRequestUrlLen = str.length();
//
//                String strInBuffer = new String("<FCSearchDescription><searchID>9988</searchID><searchResultPosition>"
//                        + pos
//                        + "</searchResultPosition><maxResults>50</maxResults><snapStartTime>" + dayInfo + "T00:00:00Z</snapStartTime>"
//                        + "<snapEndTime>" + dayInfo + "T23:59:59Z</snapEndTime><FDIDList><FDID>" + id
//                        + "</FDID></FDIDList></FCSearchDescription>");
//                HCNetSDK.BYTE_ARRAY ptrByte = new HCNetSDK.BYTE_ARRAY(10 * HCNetSDK.BYTE_ARRAY_LEN);
//                ptrByte.byValue = strInBuffer.getBytes();
//                ptrByte.write();
//                struInput.lpInBuffer = ptrByte.getPointer();
//                struInput.dwInBufferSize = strInBuffer.length();
//                struInput.write();
//
//                HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT struOutput = new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
//                struOutput.dwSize = struOutput.size();
//
//                HCNetSDK.BYTE_ARRAY ptrOutByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
//                struOutput.lpOutBuffer = ptrOutByte.getPointer();
//                struOutput.dwOutBufferSize = HCNetSDK.ISAPI_DATA_LEN;
//
//                HCNetSDK.BYTE_ARRAY ptrStatusByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
//                struOutput.lpStatusBuffer = ptrStatusByte.getPointer();
//                struOutput.dwStatusSize = HCNetSDK.ISAPI_STATUS_LEN;
//                struOutput.write();
//
//                if (hCNetSDK.NET_DVR_STDXMLConfig(lUserID, struInput, struOutput)) {
//                    String xmlStr = struOutput.lpOutBuffer.getString(0);
//
//                    // dom4j解析xml
//                    Document document = DocumentHelper.parseText(xmlStr);
//                    // 获取根节点元素对象
//                    Element FCSearchResult = document.getRootElement();
//
//                    // 同时迭代当前节点下面的所有子节点
//                    Iterator<Element> iterator = FCSearchResult.elementIterator();
//                    while (iterator.hasNext()) {
//                        Element e = iterator.next();
//
//                        if (e.getName().equals("responseStatusStrg")) {
//                            if (e.getText().equals("MORE")) {
//                                isQuit = false;
//                            } else {
//                                isQuit = true;
//                            }
//                        }
//                        if (e.getName().equals("numOfMatches")) {
//                            pos += Integer.parseInt(e.getText());
//                        }
//
//                        if (e.getName().equals("MatchList")) {
//
//                            DeviceService srvDevice = DeviceService.me; //设备业务层
//                            VisService srvVisitor = VisService.me;  //访客业务层
//                            StaffService srvStaff = StaffService.me;    //员工业务层
//                            Iterator<Element> iterator2 = e.elementIterator(); // MatchElementList节点
//                            while (iterator2.hasNext()) {
//
//                                String date = null;
//                                String time = null;
//                                String idCard = null;
//                                String name = null;
//
//                                Element e2 = iterator2.next();                    // MatchElement节点
//                                Iterator<Element> iterator3 = e2.elementIterator();
//                                while (iterator3.hasNext()) {
//                                    Element e3 = iterator3.next();
//
//                                    if (e3.getName().equals("snapTime")) {
//                                        int dateIndex = e3.getText().indexOf("T");
//
//                                        date = e3.getText().substring(0, dateIndex);
//                                        time = e3.getText().substring(dateIndex + 1);
//                                    }
//                                    if (e3.getName().equals("FaceMatchInfoList")) {
//
//                                        Iterator<Element> iterator4 = e3.elementIterator(); // FaceMatchInfoList节点
//                                        while (iterator4.hasNext()) {
//
//                                            Element e4 = iterator4.next();
//                                            if (e4.getName().equals("FaceMatchInfo")) {
//                                                Iterator<Element> iterator5 = e4.elementIterator();
//                                                while (iterator5.hasNext()) {
//
//                                                    Element e5 = iterator5.next(); // FaceMatchInfo节点
//
//                                                    if (e5.getName().equals("certificateNumber")) {
//                                                        idCard = e5.getText();
//                                                    }
//                                                    if (e5.getName().equals("name")) {
//                                                        name = e5.getText();
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                                if (name != null) {
//
//                                    if (name.contains("S")) {
//                                        TbCompanyuser companyUser = srvStaff.findOne(Integer.valueOf(idCard));
//                                        name = name.substring(1);
//                                        TbDevice device = srvDevice.findByDeviceIp(deviceIP);
//                                        System.out.println(name + "***" + Integer.valueOf(idCard) + "***" + date + "***" + time + "****" + deviceIP);
//                                        if (null == companyUser) {
//                                            log.error("员工ID与员工表关联失败，找不到" + name);
//                                            continue;
//                                        }
//                                        TbDevicerelated tbDevicerelated = srvDr.findByFaceIP(deviceIP);
//                                        saverecord(name, companyUser.getIdNO(), "staff", deviceIP, date, time, "", tbDevicerelated.getTurnOver());
//                                        //System.out.println("name：" + e5.getText());
//                                    } else if (name.contains("V")) {
//
//                                        TbVisitor visitor = srvVisitor.findVisitorId(idCard);
//                                        if (null == visitor) {
//                                            log.error("访客ID与访客表关联失败，找不到" + name);
//                                            continue;
//                                        }
//                                        TbDevice device = srvDevice.findByDeviceIp(deviceIP);
//                                        name = name.substring(1);
//                                        TbDevicerelated tbDevicerelated = srvDr.findByFaceIP(deviceIP);
//                                        saverecord(name, visitor.getVisitorIdCard(), "service", deviceIP, date, time, "", tbDevicerelated.getTurnOver());
//                                    }
//                                }
//
//
//                            }
//                        }
//                    }
//                } else {
//                    int code = hCNetSDK.NET_DVR_GetLastError();
//                    JOptionPane.showMessageDialog(null, "获取失败: " + code);
//                    log.info("IPC通行记录获取失败，失败号: " + code);
//                    return false;
//                }
//            }
//        } catch (DocumentException ex) {
//
//            return false;
//        }
//        return true;
//    }

    public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
        public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
                              Pointer pUser) {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }

    }

    //警报监听
    public void AlarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen,
                                Pointer pUser) {
        // TODO Auto-generated method stub
        String sAlarmType = new String();
        String[] newRow = new String[3];
        // 报警时间
        Date today = new Date();
        //时间格式
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        sAlarmType = new String("lCommand=") + lCommand;
        String[] sIP = new String[2];
        //门禁报警
        if (lCommand == HCNetSDK.COMM_ALARM_ACS) {
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

                TbDevice device = srv.findByDeviceIp(sIP[0]);
                TbDevicerelated tbDevicerelated = srvDr.findByFaceIP(sIP[0]);

                String username = null;
                String date1 =  String.valueOf(System.currentTimeMillis());
                if ("S".equals(idCardNo.substring(0, 1))) {
                    //员工通行
                    TbCompanyuser user = srv.findByUserId(userId);
                    String cardNO = "S" + user.getUserId();

                    username = user.getUserName();
                    //redis锁定某人，3秒内只能开启一次
                    String key = "s_" + user.getUserName() + "_" + user.getIdNO();
                    if (redisUtils.get(key) == null) {
                        redisUtils.set(key, "locked");
                        redisUtils.expire(key, 3);
                        //继电器控制
//                      Control24DeviceUtil.controlDevice(relayted.getRelayIP(), 8080, relayted.getRelayOUT(), null);
                        System.out.println("strACSInfo.byAcsEventInfoExtendV20==="+strACSInfo.byAcsEventInfoExtendV20);
                        if(strACSInfo.byAcsEventInfoExtendV20 == 1) {
                            HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                            struInfoExtend.write();
                            Pointer pInfoExtend = struInfoExtend.getPointer();
                            pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                            struInfoExtend.read();
                            if(struInfoExtend!=null){
                                saverecord(user.getUserName(), user.getIdNO(), "staff", sIP[0], date,
                                        time, cardNO, tbDevicerelated.getTurnOver(),struInfoExtend.fCurrTemperature, String.valueOf(struInfoExtend.byIsAbnomalTemperature),date1);
                            }
                         }else {
                            saverecord(user.getUserName(), user.getIdNO(), "staff", sIP[0], date,
                                    time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"",date1);
                        }
                    }
                    getPic(strACSInfo.pPicData, strACSInfo.dwPicDataLen, username,date1);
                } else if ("V".equals(idCardNo.substring(0, 1))) {
                    //访客通行
                    TbVisitor visitor = srv.findVisitorId(idCardNo.substring(1));

                    String cardNO = "V" + visitor.getVisitId();
                    username = visitor.getVisitorName();

                    List<TbVisitor> staffs = srv.findByBetweenTime(visitor.getVisitorName(), visitor.getVisitorIdCard(), getDateTime());
                    if (staffs.size() > 0) {
                        String key = "v_" + visitor.getVisitorName() + "_" + visitor.getVisitorIdCard();
                        //redis锁定某人，2秒内只能开启一次
                        if (redisUtils.get(key) == null) {
                            redisUtils.set(key, "locked");
                            redisUtils.expire(key, 3);
                            //继电器控制
//                      Control24DeviceUtil.controlDevice(relayted.getRelayIP(), 8080, relayted.getRelayOUT(), null);

                            if(strACSInfo.byAcsEventInfoExtendV20 == 1) {
                                HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                                struInfoExtend.write();
                                Pointer pInfoExtend = struInfoExtend.getPointer();
                                pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                                struInfoExtend.read();

                                saverecord(visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor", sIP[0],
                                        date, time, cardNO, tbDevicerelated.getTurnOver(),struInfoExtend.fCurrTemperature, String.valueOf(struInfoExtend.byIsAbnomalTemperature),date1);
                            }else {
                                saverecord(visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor", sIP[0],
                                        date, time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"",date1);
                            }
                        }
                    } else {
                        log.error(visitor.getVisitorName() + "不在有效访问时间");
                    }
                    getPic(strACSInfo.pPicData, strACSInfo.dwPicDataLen, username,date1);
                }

                String scanTime = year + month + day + hour + minute + second;
                //getPic(strACSInfo.pPicData, strACSInfo.dwPicDataLen, idCardNo, scanTime);


            }
            String cardNo = new String(strACSInfo.struAcsEventInfo.byCardNo).trim(); //卡号
            sAlarmType = sAlarmType + "：门禁主机报警信息，卡号：" + cardNo
                    + "，卡类型：" + strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型："
                    + strACSInfo.dwMinor;
            if(strACSInfo.byAcsEventInfoExtendV20 == 1)
            {
                HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 struInfoExtend = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                struInfoExtend.write();
                Pointer pInfoExtend = struInfoExtend.getPointer();
                pInfoExtend.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, struInfoExtend.size()), 0, struInfoExtend.size());
                struInfoExtend.read();
                sAlarmType = sAlarmType + ", 温度:" + struInfoExtend.fCurrTemperature + ", 是否异常:" + struInfoExtend.byIsAbnomalTemperature;
            }
            newRow[0] = dateFormat.format(today);
            // 报警类型
            newRow[1] = sAlarmType;
            // 报警设备IP地址
            log.info(sAlarmType);
        }
//        } else if (lCommand == HCNetSDK.COMM_SNAP_MATCH_ALARM) {
//            //人脸黑名单比对报警
//            HCNetSDK.NET_VCA_FACESNAP_MATCH_ALARM strFaceSnapMatch = new HCNetSDK.NET_VCA_FACESNAP_MATCH_ALARM();
//            strFaceSnapMatch.write();
//            Pointer pFaceSnapMatch = strFaceSnapMatch.getPointer();
//            pFaceSnapMatch.write(0, pAlarmInfo.getByteArray(0, strFaceSnapMatch.size()), 0, strFaceSnapMatch.size());
//            strFaceSnapMatch.read();
//            try {
//                //比对人姓名
//                String userName = new String(strFaceSnapMatch.struBlackListInfo.struBlackListInfo.struAttribute.byName, "GBK").trim();
//                float similarity = strFaceSnapMatch.fSimilarity * 100;
//                //网络摄像头员工使用的证件号为userId字段，通过usrId可找到该员工信息，访客使用visitId，通过visitId可找到访问记录
//                String idstr = new String(strFaceSnapMatch.struBlackListInfo.struBlackListInfo.struAttribute.byCertificateNumber).trim();
//
//                float towerCope = Float.valueOf(srv.findFaceComparesCope());
//                System.out.println(userName + "********" + similarity + "***********" + idstr + "*******" + towerCope);
//                //人脸比对值小于大楼设置阈值
//                if (similarity < towerCope) {
//                    log.error(userName + "比对值小于阈值");
//                } else if (null != userName && !"".equals(userName)) {
//
//                    //SIP[0]为设备IP地址,根据该设备IP查找设备关系表，寻找继电器信息
//                    sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
//                    TbDevicerelated devRelated = srv.findByFaceIP(sIP[0]);
//
//                    if (userName.contains("S")) {
//
//                        int userId = Integer.valueOf(idstr);
//                        TbCompanyuser user = srv.findByUserId(userId);
//                        if (null != user) {
//                            String key = "s_" + user.getUserName() + "_" + user.getIdNO();
//                            if (redisUtils.get(key) == null) {
//                                redisUtils.set(key, "locked");
//                                redisUtils.expire(key, 3);
//                                Control24DeviceUtil.controlDevice(devRelated.getRelayIP(), 8080, devRelated.getRelayOUT(), null);
//                                saverecord(user.getUserName(), user.getIdNO(), "staff", sIP[0], devRelated.getRelayOUT(), getDate(),
//                                        getTime(), "");
//                            }
//                        } else {
//                            log.error("对应员工表找不到userId为" + userId + "的字段");
//                        }
//
//                    } else if (userName.contains("V")) {
//
//                        //根据visitorId查找访客信息
//                        String visitId = idstr;
//
//                        TbVisitor service = srv.findVisitorId(visitId);
//
//                        List<TbVisitor> visitors = srv.findByBetweenTime(service.getVisitorName(), service.getVisitorIdCard(), getDateTime());
//
//
//                        if (visitors.size() > 0) {
//                            String key = "v_" + service.getVisitorName() + "_" + service.getVisitorIdCard();
//                            if (redisUtils.get(key) == null) {
//                                redisUtils.set(key, "locked");
//                                redisUtils.expire(key, 3);
//                                Control24DeviceUtil.controlDevice(devRelated.getRelayIP(), 8080, devRelated.getRelayOUT(), null);
//                                saverecord(service.getVisitorName(), service.getVisitorIdCard(), "service", sIP[0],
//                                        devRelated.getRelayOUT(), getDate(), getTime(), "");
//                            }
//                        } else {
//                            log.error(service.getVisitorName() + "不在有效访问时间");
//                        }
//                    }
//
//                }
//            } catch (UnsupportedEncodingException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }

    }

    /**
     * 门禁通行异常回调（长连接失败重连）
     */
    class ExceptionCallBack implements HCNetSDK.FExceptionCallBack {

        public void invoke(int dwType, int lUserID, int lHandle, Pointer pUser) {
            // TODO Auto-generated method stub
            switch (dwType) {
                case HCNetSDK.EXCEPTION_ALARMRECONNECT:
                    log.info("门禁通行长连接尝试重连...");
                    HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
                    m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
                    m_strAlarmInfo.byLevel = 1;
                    m_strAlarmInfo.byAlarmInfoType = 1;
                    m_strAlarmInfo.bySupport = 1;
                    m_strAlarmInfo.byDeployType = 1;
                    m_strAlarmInfo.write();
                    int suc = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
                    if (suc < 0) {
                        log.error("门禁通行长连接重连失败");

                    } else {
                        log.info("门禁通行长连接重连成功");
                        hCNetSDK.NET_DVR_CloseAlarmChan_V30(suc);
                    }
                    break;

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
    private void getPic(Pointer picData, int dwPicDataLen, String username, String date1) {
        if (dwPicDataLen > 0) {

            File file = new File(Constants.AccessRecPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            FileOutputStream fout;

            try {

//                fout = new FileOutputStream(Constants.AccessRecPath+ "/" + username+"_"+getDateTime() + ".jpg");
                fout = new FileOutputStream(Constants.AccessRecPath+ "/" + username+"_"+date1 + ".jpg");
                long offset = 0;
                ByteBuffer buffers = picData.getByteBuffer(offset, dwPicDataLen);
                byte[] bytes = new byte[dwPicDataLen];
                buffers.rewind();
                buffers.get(bytes);
                fout.write(bytes);
                fout.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }


    // 订阅句柄
    public static LLong m_hAttachHandle = new LLong(0);
    public AnalyzerDataCB analyzerCallback = new AnalyzerDataCB();
    public static String ip = null;

    /**
     * 大华设备长接连
     */
    public void dhSendAccessRecord(String deviceIp) {
        //登录
        ip = deviceIp;
        LoginModule.login(ip, 37777, Constants.deviceLoginName, Constants.deviceLoginPassWord);
        m_hAttachHandle = realLoadPic(analyzerCallback);
        if (m_hAttachHandle.longValue() != 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(null, ToolKits.getErrorCodeShow(), Res.string().getErrorMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 订阅实时上传智能分析数据
     *
     * @return
     */
    private NetSDKLib.LLong realLoadPic(NetSDKLib.fAnalyzerDataCallBack m_AnalyzerDataCB) {

        /**
         * 说明：
         * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
         *  下列仅订阅了0通道的智能事件.
         */
        int ChannelId = 0;      //通道号
        int bNeedPicture = 0; // 是否需要图片

        LLong m_hAttachHandle = LoginModule.netsdk.CLIENT_RealLoadPictureEx(LoginModule.m_hLoginHandle, ChannelId, NetSDKLib.EVENT_IVS_ALL,
                bNeedPicture, m_AnalyzerDataCB, null, null);
        if (m_hAttachHandle.longValue() != 0) {
            log.info("CLIENT_RealLoadPictureEx Success  ChannelId : " + ChannelId);
        } else {
            log.error("CLIENT_RealLoadPictureEx Failed!" + ToolKits.getErrorCodePrint());
            return null;
        }
        return m_hAttachHandle;
    }


    public class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private BufferedImage gateBufferedImage = null;

        public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            File path = new File("./GateSnapPicture/");
            if (!path.exists()) {
                path.mkdir();
            }

            ///< 门禁事件
            if (dwAlarmType == NetSDKLib.EVENT_IVS_ACCESS_CTL) {
                DEV_EVENT_ACCESS_CTL_INFO msg = new DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                // 保存图片，获取图片缓存
                String snapPicPath = path + "\\" + System.currentTimeMillis() + "GateSnapPicture.jpg";  // 保存图片地址
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
                // 通行记录显示
                EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                if (eventQueue != null) {
                    // 时间
                    if (msg.UTC == null || msg.UTC.toString().isEmpty()) {
                        System.out.print("");

                    } else {
                        System.out.print("时间:" + DateUtil.addDateMinut(msg.UTC.toString(), 8));
                    }

                    // 开门状态
                    if (msg.bStatus == 1) {
                        System.out.print("--开门状态:" + Res.string().getSucceed());
                    } else {
                        System.out.print("--开门状态:" + Res.string().getFailed());
                    }

                    // 开门方式
                    System.out.print("--开门方式:" + Res.string().getOpenMethods(msg.emOpenMethod));

                    // 卡名
                    try {
                        System.out.print("--卡名:" + new String(msg.szCardName, "GBK").trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    // 卡号
                    System.out.print("--卡号:" + new String(msg.szCardNo).trim());

                    //用户ID
                    System.out.println("--用户ID:" + new String(msg.szUserID).trim());

                    //保存大华设备的通行记录
                    String dateSplit = DateUtil.addDateMinut(msg.UTC.toString(), 8);
                    String[] split = dateSplit.split("\\s");
                    String cardNO = new String(msg.szCardNo).trim();  //卡号
                    String date = split[0];
                    String time = split[1];
                    TbDevice device = srv.findByDeviceIp(ip);
                    TbDevicerelated tbDevicerelated = srvDr.findByFaceIP(ip);
                    String userId = (new String(msg.szUserID).trim());
                    TbCompanyuser user = srv.findByUserId(Integer.parseInt(userId));
                    //员工通行记录
                    if ("S".equals(cardNO.substring(0, 1))) {
                        saverecord(user.getUserName(), user.getIdNO(), "staff", ip, date,
                                time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"","");
                    }
                    //访客通行记录
                    else if ("V".equals(cardNO.substring(0, 1))) {
                        saverecord(user.getUserName(), user.getIdNO(), "service", ip, date,
                                time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"","");
                    }
                    //共享方式通行记录
                    else if ("G".equals(cardNO.substring(0, 1))) {
                        saverecord(user.getUserName(), user.getIdNO(), "share", ip, date,
                                time, cardNO, tbDevicerelated.getTurnOver(),0.00F,"","");
                    }
                }
            }

            return 0;
        }
    }

    class AccessEvent extends AWTEvent {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;

        private BufferedImage gateBufferedImage = null;
        private DEV_EVENT_ACCESS_CTL_INFO msg = null;

        public AccessEvent(Object target,
                           BufferedImage gateBufferedImage,
                           DEV_EVENT_ACCESS_CTL_INFO msg) {
            super(target, EVENT_ID);
            this.gateBufferedImage = gateBufferedImage;
            this.msg = msg;
        }
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
                           String time, String cardNO, String out,float Temperature,String Abnomal,String date1) {
        // TODO Auto-generated method stub
        TbAccessrecord accessRecord = new TbAccessrecord();
        accessRecord.setPhoto("/Recored/"+name+"_"+date1 + ".jpg");
        accessRecord.setOrgCode(ServerService.me.findByOrgCode());
        accessRecord.setPospCode(ServerService.me.findPospCode());
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

    //姓名隐藏
    private String AccordingToName(String username) {
        StringBuffer name = null;
        if (username.length() == 1 || username.length() > 5) {
            return username;
        }else if (username.length() >= 2 && username.length() <= 3) {
            name = new StringBuffer(username);
            //创建StringBuffer对象strb
            name.setCharAt(1, '*');    //修改指定位置的字符
            //输出strb 的长度
            name.setLength(6);      //设置字符串长度，超出部分会被裁剪
        }else if (username.length() >= 4) {
            name = new StringBuffer(username);
            //创建StringBuffer对象strb
            name.setCharAt(1, '*');    //修改指定位置的字符
            name.setCharAt(2, '*');    //修改指定位置的字符
            //输出strb 的长度
            name.setLength(6);
        }
        String str = new String(name);
        return str;
    }

    /**
     * 下发 海康设备 人脸信息
     *
     * @param deviceIP    设备ip
     * @param companyUser 用户
     * @return
     */
    public boolean setCardAndFace(String deviceIP, TbCompanyuser companyUser, TbVisitor visitor) {
        // TODO Auto-generated method stub
        lUserID = initAndLogin(deviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord);
        String strCardNo;    //用户卡号
        int dwEmployeeNo;    //用户工号
        String name;        //用户名字
        String startTime = "";
        String endTime = "";
        if (null == companyUser) {
            strCardNo = "V" + visitor.getUserId();
            dwEmployeeNo = Integer.valueOf(visitor.getUserId());
            name = visitor.getVisitorName();
            startTime = visitor.getStartDateTime();
            endTime = visitor.getEndDateTime();
        } else {
            strCardNo = "S" + String.valueOf(companyUser.getUserId());
            dwEmployeeNo = companyUser.getUserId();
            name = companyUser.getUserName();
        }
        if (lUserID < 0) {
            hCNetSDK.NET_DVR_Logout(lUserID);
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
        fRemoteCfgCallBackCardSet = new FRemoteCfgCallBackCardSet();
        int cardHandle = this.hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_CFG_V50, cardInBuffer,
                m_struCardInputParamSet.size(), fRemoteCfgCallBackCardSet, cardUserData);
        if (cardHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("建立长连接失败，错误号：" + iErr);
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
        if (visitor != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                //针对软件园8号楼设备快20分钟
                //endDate.setTime(endDate.getTime() + 20*60*1000);
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
                log.info("通行日期："+struCardInfo.struValid.struEndTime.wYear + "*****" + struCardInfo.struValid.struEndTime.byMonth + "**" + struCardInfo.struValid.struEndTime.byDay);
                log.info("通行时间:"+struCardInfo.struValid.struEndTime.byHour + "**" + struCardInfo.struValid.struEndTime.byMinute);
            } catch (Exception e2) {
                // TODO Auto-generated catch block
                hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
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
            log.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        if (!hCNetSDK.NET_DVR_StopRemoteConfig(cardHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
            System.out.println("断开长连接失败，错误号：" + iErr);
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
        fRemoteCfgCallBackFaceSet = new FRemoteCfgCallBackFaceSet();

        int lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE_PARAM_CFG, faceInBuffer,
                m_struFaceSetParam.size(), fRemoteCfgCallBackFaceSet, faceUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("建立长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
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
            if (companyUser != null) {
                String filePath = companyUser.getPhoto();

                File picture = new File(filePath);
                if (!picture.exists()) {
                    hCNetSDK.NET_DVR_Logout(lUserID);
                    log.error("照片路径不存在");
                    return false;
                }
                picfile = new FileInputStream(picture);
            } else {
                String filePath = visitor.getPhoto();

                File picture = new File(filePath);
                if (!picture.exists()) {
                    hCNetSDK.NET_DVR_Logout(lUserID);
                    log.error("照片路径不存在");
                    return false;
                }
                picfile = new FileInputStream(picture);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            picdataLength = picfile.available();
        } catch (IOException e1) {
            hCNetSDK.NET_DVR_Logout(lUserID);
            e1.printStackTrace();
        }
        if (picdataLength < 0) {
            log.error("input file dataSize < 0");
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        try {
            picfile.read(ptrpicByte.byValue);
        } catch (IOException e2) {
            hCNetSDK.NET_DVR_Logout(lUserID);
            e2.printStackTrace();
        }
        ptrpicByte.write();
        struFaceInfo.dwFaceLen = picdataLength;
        struFaceInfo.pFaceBuffer = ptrpicByte.getPointer();

        struFaceInfo.write();
        Pointer pSendBufSet = struFaceInfo.getPointer();
        if (!hCNetSDK.NET_DVR_SendRemoteConfig(lHandle, 0x9, pSendBufSet, struFaceInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("NET_DVR_SendRemoteConfig失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            hCNetSDK.NET_DVR_Logout(lUserID);
            e.printStackTrace();
        }
        System.out.println("fRemoteCfgCallBackFaceSet.isFlag" + fRemoteCfgCallBackFaceSet.isFlag);
        if (fRemoteCfgCallBackFaceSet.isFlag != 1) {
            log.error("下发人脸参数失败,错误号:"+hCNetSDK.NET_DVR_GetLastError());
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        if (!hCNetSDK.NET_DVR_StopRemoteConfig(lHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }

        try {
            picfile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("文件流未打开");
            hCNetSDK.NET_DVR_Logout(lUserID);
            e.printStackTrace();
        }
        hCNetSDK.NET_DVR_Logout(lUserID);
        return true;
    }

    /**
     * 添加卡信息
     *
     * @param deviceIP     设备ip
     * @param dwEmployeeNo 公司ud
     * @param name         用户名
     * @param strCardNo    卡号
     * @return
     * @throws UnsupportedEncodingException
     */
    public boolean setCardInfo(String deviceIP, int dwEmployeeNo, String name, String strCardNo, String startTime, String endTime)
            throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        lUserID = initAndLogin(deviceIP, Constants.deviceLoginName1, Constants.deviceLoginPassWord1);
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

        Pointer lpInBuffer = m_struCardInputParamSet.getPointer();
        m_struCardInputParamSet.write();

        Pointer pUserData = null;
        FRemoteCfgCallBackCardSet fRemoteCfgCallBackCardSet = new FRemoteCfgCallBackCardSet();

        int lHandle = this.hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_CFG_V50, lpInBuffer,
                m_struCardInputParamSet.size(), fRemoteCfgCallBackCardSet, pUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("建立长连接失败，错误号：" + iErr);
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
        struCardInfo.byDoorRight[1] = 1; // 门2有权限
        struCardInfo.wCardRightPlan[0].wRightPlan[0] = 1; // 门1关联卡参数计划模板1
        struCardInfo.wCardRightPlan[1].wRightPlan[0] = 1; // 门2关联卡参数计划模板1

        // 卡有效期,员工无期限，访客限制时间
        String substring = strCardNo.substring(0, 1);
        if (substring.equals("V")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                //针对软件园8号楼设备快20分钟
                endDate.setTime(endDate.getTime() + 20*60*1000);
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
                System.out.println(struCardInfo.struValid.struEndTime.wYear + "*****" + struCardInfo.struValid.struEndTime.byMonth + "**" + struCardInfo.struValid.struEndTime.byDay);
                System.out.println(struCardInfo.struValid.struEndTime.byHour + "**" + struCardInfo.struValid.struEndTime.byMinute);
            } catch (Exception e2) {
                // TODO Auto-generated catch block
                hCNetSDK.NET_DVR_Logout(lUserID);    //注销登录
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
        Pointer pSendBufSet = struCardInfo.getPointer();

        if (!hCNetSDK.NET_DVR_SendRemoteConfig(lHandle, 0x3, pSendBufSet, struCardInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
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
            log.error("断开长连接失败，错误号：" + iErr);
            hCNetSDK.NET_DVR_Logout(lUserID);
            return false;
        }
        hCNetSDK.NET_DVR_Logout(lUserID);
        log.error("断开长连接成功!");
        return true;
    }

    /**
     * 对IPC网络摄像头下发数据
     */
    public boolean sendToIPC(String hcDeviceIP, File picture, File picAppendData, TbCompanyuser companyUser,
                             TbVisitor visitor) {
        // TODO Auto-generated method stub
        if (initAndLogin(hcDeviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord) < 0) {
            return false;
        }
        m_lUploadHandle = UploadFile("1");

        if (m_lUploadHandle.longValue() != 0) {
            return false;
        }
        boolean result = UploadFaceLinData(picture, picAppendData, companyUser, visitor);

        try {
            new Thread().sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;

    }

    /***
     *
     * 	删除IPC摄像头里的人像照片
     *
     *
     */
    public boolean delIPCpicture(String type, String picID) {
        // TODO Auto-generated method stub

        String str = "DELETE /ISAPI/Intelligent/FDLib/1/picture/" + picID;

        HCNetSDK.NET_DVR_XML_CONFIG_INPUT struInput = new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
        struInput.dwSize = struInput.size();

        HCNetSDK.BYTE_ARRAY ptrDeleteFaceLibUrl = new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
        System.arraycopy(str.getBytes(), 0, ptrDeleteFaceLibUrl.byValue, 0, str.length());
        ptrDeleteFaceLibUrl.write();
        struInput.lpRequestUrl = ptrDeleteFaceLibUrl.getPointer();
        struInput.dwRequestUrlLen = str.length();
        struInput.lpInBuffer = null;
        struInput.dwInBufferSize = 0;
        struInput.write();

        HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT struOutput = new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
        struOutput.dwSize = struOutput.size();
        struOutput.lpOutBuffer = null;
        struOutput.dwOutBufferSize = 0;

        HCNetSDK.BYTE_ARRAY ptrStatusByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
        struOutput.lpStatusBuffer = ptrStatusByte.getPointer();
        struOutput.dwStatusSize = HCNetSDK.ISAPI_STATUS_LEN;
        struOutput.write();

        if (!hCNetSDK.NET_DVR_STDXMLConfig(lUserID, struInput, struOutput)) {
            log.info("NET_DVR_STDXMLConfig DELETE failed with:" + " " + hCNetSDK.NET_DVR_GetLastError());
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        } else {
            log.info("NET_DVR_STDXMLConfig DELETE Succ!!!!!!!!!!!!!!!");
            log.info("图片删除成功 PID:" + picID);
            return true;
        }

    }


    /**
     * 网络摄像头 与人脸库建立长连接
     *
     * @param faceLib 人脸库ID
     * @return
     */
    private NativeLong UploadFile(String faceLib) {
        HCNetSDK.NET_DVR_FACELIB_COND struInput = new HCNetSDK.NET_DVR_FACELIB_COND();
        struInput.dwSize = struInput.size();
        struInput.szFDID = faceLib.getBytes();
        struInput.byConcurrent = 0;
        struInput.byCover = 1;
        struInput.byCustomFaceLibID = 0;
        struInput.write();
        Pointer lpInput = struInput.getPointer();
        NativeLong ret = hCNetSDK.NET_DVR_UploadFile_V40(lUserID, HCNetSDK.IMPORT_DATA_TO_FACELIB, lpInput,
                struInput.size(), null, null, 0);
        System.out.println("m_lUploadHandle:" + ret.intValue());
        if (ret.longValue() == -1) {
            int code = hCNetSDK.NET_DVR_GetLastError();
            log.error("上传图片文件失败: " + code);
            return ret;
        } else {
            return ret;
        }

    }

    /**
     * @param picture       图片
     * @param picAppendData 图片附加信息
     */
    public boolean UploadFaceLinData(File picture, File picAppendData, TbCompanyuser companyUser, TbVisitor visitor) {

        UploadSend(picture, picAppendData);
        while (true) {
            if (-1 == m_lUploadHandle.longValue()) {
                return false;
            }
            m_UploadStatus = getUploadState();
            if (m_UploadStatus.longValue() == 1) {
                HCNetSDK.NET_DVR_UPLOAD_FILE_RET struPicRet = new HCNetSDK.NET_DVR_UPLOAD_FILE_RET();
                struPicRet.write();
                Pointer lpPic = struPicRet.getPointer();

                boolean bRet = hCNetSDK.NET_DVR_GetUploadResult(m_lUploadHandle, lpPic, struPicRet.size());
                if (!bRet) {
                    System.out.println("NET_DVR_GetUploadResult failed with:" + hCNetSDK.NET_DVR_GetLastError());
                    if (hCNetSDK.NET_DVR_UploadClose(m_lUploadHandle)) {
                        m_lUploadHandle.setValue(-1);
                    }
                    return false;
                } else {
                    System.out.println("NET_DVR_GetUploadResult succ");
                    struPicRet.read();
                    String m_picID = new String(struPicRet.sUrl);
                    System.out.println("图片上传成功 PID:" + m_picID);
                    if (null != companyUser) {
//                        companyUser.setIdFrontImgUrl(m_picID);
                        companyUser.save();
                    }
                    if (null != visitor) {
                        visitor.setIdFrontImgUrl(m_picID);
                        visitor.save();
                    }

                    if (hCNetSDK.NET_DVR_UploadClose(m_lUploadHandle)) {
                        m_lUploadHandle.setValue(-1);
                    }
                    return true;
                }

            } else if (m_UploadStatus.longValue() >= 3 || m_UploadStatus.longValue() == -1) {
                System.out.println("m_UploadStatus = " + m_UploadStatus);
                hCNetSDK.NET_DVR_UploadClose(m_lUploadHandle);
                m_lUploadHandle.setValue(-1);
                return false;
            }
        }

    }

    /**
     * 网络摄像头 获取文件上传进度
     *
     * @return
     */
    public NativeLong getUploadState() {
        IntByReference pInt = new IntByReference(0);
        m_UploadStatus = hCNetSDK.NET_DVR_GetUploadState(m_lUploadHandle, pInt);
        if (m_UploadStatus.longValue() == -1) {
            System.out.println("NET_DVR_GetUploadState fail,error=" + hCNetSDK.NET_DVR_GetLastError());
            log.error("下发人脸及附加信息失败，错误号=" + hCNetSDK.NET_DVR_GetLastError());
        } else if (m_UploadStatus.longValue() == 2) {
            // System.out.println("is uploading!!!! progress = " + pInt.getValue());
        } else if (m_UploadStatus.longValue() == 1) {
            log.info("下发成功");
        } else {
            log.error("下发失败，失败号=" + hCNetSDK.NET_DVR_GetLastError());
        }

        return m_UploadStatus;
    }

    /**
     * 网络摄像头 上传图片及图片的附加信息
     *
     * @param picture       jpg格式图片
     * @param picAppendData xml格式附加文件
     * @param
     */
    public void UploadSend(File picture, File picAppendData) {
        FileInputStream picfile = null;
        FileInputStream xmlfile = null;
        int picdataLength = 0;
        int xmldataLength = 0;

        try {
            picfile = new FileInputStream(picture);
            xmlfile = new FileInputStream(picAppendData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            picdataLength = picfile.available();
            xmldataLength = xmlfile.available();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        if (picdataLength < 0 || xmldataLength < 0) {
            System.out.println("input file/xml dataSize < 0");
            return;
        }

        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        HCNetSDK.BYTE_ARRAY ptrxmlByte = new HCNetSDK.BYTE_ARRAY(xmldataLength);

        try {
            picfile.read(ptrpicByte.byValue);
            xmlfile.read(ptrxmlByte.byValue);
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        ptrpicByte.write();
        ptrxmlByte.write();

        HCNetSDK.NET_DVR_SEND_PARAM_IN struSendParam = new HCNetSDK.NET_DVR_SEND_PARAM_IN();

        struSendParam.pSendData = ptrpicByte.getPointer();
        struSendParam.dwSendDataLen = picdataLength;
        struSendParam.pSendAppendData = ptrxmlByte.getPointer();
        struSendParam.dwSendAppendDataLen = xmldataLength;
        if (struSendParam.pSendData == null || struSendParam.pSendAppendData == null || struSendParam.dwSendDataLen == 0
                || struSendParam.dwSendAppendDataLen == 0) {
            System.out.println("input file/xml data err");
            return;
        }

        struSendParam.byPicType = 1;
        struSendParam.dwPicMangeNo = 0;
        struSendParam.write();

        NativeLong iRet = hCNetSDK.NET_DVR_UploadSend(m_lUploadHandle, struSendParam.getPointer(), null);

        System.out.println("iRet=" + iRet);
        if (iRet.longValue() < 0) {
            System.out.println("NET_DVR_UploadSend fail,error=" + hCNetSDK.NET_DVR_GetLastError());
        } else {
            System.out.println("NET_DVR_UploadSend success");
            System.out.println("dwSendDataLen =" + struSendParam.dwSendDataLen);
            System.out.println("dwSendAppendDataLen =" + struSendParam.dwSendAppendDataLen);
        }

        try {
            picfile.close();
            xmlfile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public boolean getCardInfo(String deviceIP) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        return false;
    }

    /***
     * hCNetSDK：海康SDK lUserID：SDK注册状态（-1失败，0成功） isdel：删除还是下发的标志（delete删除，normal下发）
     * service：访客数据
     *
     */
    public boolean setVisitorCard(String deviceIP, TbVisitor visitor) {
        // TODO Auto-generated method stub

        lUserID = initAndLogin(deviceIP, Constants.deviceLoginName, Constants.deviceLoginPassWord);
        if (lUserID < 0) {
            log.error("注册失败");
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
        fRemoteCfgCallBackCardSet = new FRemoteCfgCallBackCardSet();

        int lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD_CFG_V50, lpInBuffer,
                m_struCardInputParamSet.size(), fRemoteCfgCallBackCardSet, pUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("建立长连接失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
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
        String strCardNo = "V" + visitor.getVisitId();
        for (int i = 0; i < strCardNo.length(); i++) {
            struCardInfo.byCardNo[i] = strCardNo.getBytes()[i];
        }

        struCardInfo.byCardValid = 0;
        struCardInfo.byCardValid = 1;

        struCardInfo.byCardType = 1;
        struCardInfo.byLeaderCard = 0;
        struCardInfo.byDoorRight[0] = 1; // 门1有权限
        struCardInfo.wCardRightPlan[0].wRightPlan[0] = 1; // 门1关联卡参数计划模板1

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date startDate = sdf.parse(visitor.getStartDateTime());
            Date endDate = sdf.parse(visitor.getEndDateTime());
            Calendar startCalendar = Calendar.getInstance();
            Calendar endCalendar = Calendar.getInstance();
            startCalendar.setTime(startDate);
            endCalendar.setTime(endDate);

            // 卡有效期
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
            System.out.println(struCardInfo.struValid.struEndTime.wYear + "*****" + struCardInfo.struValid.struEndTime.byMonth + "**" + struCardInfo.struValid.struEndTime.byDay);
            System.out.println(struCardInfo.struValid.struEndTime.byHour + "**" + struCardInfo.struValid.struEndTime.byMinute);
        } catch (Exception e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        struCardInfo.dwMaxSwipeTime = 0; // 无次数限制
        struCardInfo.dwSwipeTime = 0;
        struCardInfo.byCardPassword = "123456".getBytes();
        struCardInfo.dwEmployeeNo = Integer.valueOf(visitor.getVisitId());
        struCardInfo.wSchedulePlanNo = 1;
        struCardInfo.bySchedulePlanType = 2;
        struCardInfo.wDepartmentNo = 1;

        byte[] strCardName = null;
        try {
            strCardName = visitor.getVisitorName().getBytes("GBK");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
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
            log.error("ENUM_ACS_SEND_DATA失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (fRemoteCfgCallBackCardSet.sendFlag != 1) {
            return false;
        }
        if (!hCNetSDK.NET_DVR_StopRemoteConfig(lHandle)) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("断开长连接失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        }
        hCNetSDK.NET_DVR_Logout(lUserID);
        return true;
    }

    public boolean setVisitorFace(String deviceIP, TbVisitor visitor) {
        // TODO Auto-generated method stub
//        lUserID = initAndLogin(deviceIP);
        int iErr = 0; // 错误号

        // 设置人脸参数
        HCNetSDK.NET_DVR_FACE_PARAM_COND m_struFaceSetParam = new HCNetSDK.NET_DVR_FACE_PARAM_COND();
        m_struFaceSetParam.dwSize = m_struFaceSetParam.size();

        String strCardNo = "V" + visitor.getVisitId();
        // String strCardNo = "201909";// 人脸关联的卡号
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            m_struFaceSetParam.byCardNo[i] = 0;
        }

        System.arraycopy(strCardNo.getBytes(), 0, m_struFaceSetParam.byCardNo, 0, strCardNo.length());

        m_struFaceSetParam.byEnableCardReader[0] = 1;
        m_struFaceSetParam.dwFaceNum = 1;
        m_struFaceSetParam.byFaceID = 1;
        m_struFaceSetParam.write();

        Pointer lpInBuffer = m_struFaceSetParam.getPointer();

        Pointer pUserData = null;
        fRemoteCfgCallBackFaceSet = new FRemoteCfgCallBackFaceSet();

        int lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE_PARAM_CFG, lpInBuffer,
                m_struFaceSetParam.size(), fRemoteCfgCallBackFaceSet, pUserData);
        if (lHandle < 0) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("建立长连接失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        }
        HCNetSDK.NET_DVR_FACE_PARAM_CFG struFaceInfo = new HCNetSDK.NET_DVR_FACE_PARAM_CFG(); // 卡参数
        struFaceInfo.read();
        struFaceInfo.dwSize = struFaceInfo.size();

        // strCardNo = "201909";// 人脸关联的卡号
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
            String filePath = visitor.getPhoto();
            System.out.println("照片路径:"+filePath);
            File picture = new File(filePath);
            if (!picture.exists()) {
                log.error("访客" + visitor.getVisitorName() + "照片不存在");
                boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
                if (logout) {
                    log.info("注销成功");
                } else {
                    log.error("注销失败");
                }
                return false;
            }
            // picfile = new FileInputStream(new File(System.getProperty("user.dir") +
            // "\\face.jpg"));
            picfile = new FileInputStream(picture);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            picdataLength = picfile.available();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (picdataLength < 0) {
            log.error("人脸照片数据长度错误");
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        }

        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        try {
            picfile.read(ptrpicByte.byValue);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        ptrpicByte.write();
        /**************************/

        struFaceInfo.dwFaceLen = picdataLength;
        struFaceInfo.pFaceBuffer = ptrpicByte.getPointer();

        struFaceInfo.write();
        Pointer pSendBufSet = struFaceInfo.getPointer();

        // ENUM_ACS_INTELLIGENT_IDENTITY_DATA = 9, //智能身份识别终端数据类型，下发人脸图片数据
        if (!hCNetSDK.NET_DVR_SendRemoteConfig(lHandle, 0x9, pSendBufSet, struFaceInfo.size())) {
            iErr = hCNetSDK.NET_DVR_GetLastError();
            log.error("NET_DVR_SendRemoteConfig失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
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
            log.error("断开长连接失败，错误号：" + iErr);
            boolean logout = hCNetSDK.NET_DVR_Logout(lUserID);
            if (logout) {
                log.info("注销成功");
            } else {
                log.error("注销失败");
            }
            return false;
        }
        hCNetSDK.NET_DVR_Logout(lUserID);
        return true;
    }

    /***
     *
     * 门禁设备人脸下发回调函数
     * @author Admin
     *
     */
    class FRemoteCfgCallBackFaceSet implements HCNetSDK.FRemoteConfigCallback {

        public int isFlag = -1;        //读卡器状态

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
                            log.info("下发人脸参数成功,dwStatus:" + iStatus);
                            break;
                        case 1001:
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 4, byCardNo, 0, 32);
                            //System.out.println("正在下发人脸参数中,dwStatus:" + iStatus + ",卡号:" + new String(byCardNo).trim());
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
                            //System.out.println("下发人脸参数失败, dwStatus:" + iStatus + ",错误号:" + iErrorCode + ",卡号:"+ new String(byCardNo).trim());

                            break;
                    }
                    break;
                case 2:// 获取状态数据
                    HCNetSDK.NET_DVR_FACE_PARAM_STATUS m_struFaceStatus = new HCNetSDK.NET_DVR_FACE_PARAM_STATUS();
                    m_struFaceStatus.write();
                    Pointer pStatusInfo = m_struFaceStatus.getPointer();
                    pStatusInfo.write(0, lpBuffer.getByteArray(0, m_struFaceStatus.size()), 0, m_struFaceStatus.size());
                    m_struFaceStatus.read();
                    String str = new String(m_struFaceStatus.byCardNo).trim();
                    log.info("下发人脸数据关联的卡号:" + str + ",人脸读卡器状态:" + m_struFaceStatus.byCardReaderRecvStatus[0]
                            + ",错误描述:" + new String(m_struFaceStatus.byErrorMsg).trim());
                    //System.out.println(m_struFaceStatus.byCardReaderRecvStatus[0]);
                    if (m_struFaceStatus.byCardReaderRecvStatus[0] == 1 || m_struFaceStatus.byCardReaderRecvStatus[0] == 4 || m_struFaceStatus.byCardReaderRecvStatus[0] == 7 ) {
                        isFlag = 1;
                    } else {
                        isFlag = -1;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * 	下发卡回调函数
     *
     *
     */
    class FRemoteCfgCallBackCardSet implements HCNetSDK.FRemoteConfigCallback {

        public int sendFlag = -1;        //卡状态下发返回标记（1成功，-1失败,0正在下发）

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
                            log.info("下发卡参数成功");
                            sendFlag = 1;
                            break;
                        case 1001:
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 4, byCardNo, 0, 32);
                            log.info("正在下发卡参数中,卡号:" + new String(byCardNo).trim());
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
                            log.error("下发卡参数失败,卡号:" + new String(byCardNo).trim() + ",错误号:" + iErrorCode);
                            sendFlag = -1;
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }


    //获取当前时间的  年-月-日  时:分:秒
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    //获取 当前时间的 年-月-日
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    //获取 当前时间的 时:分:秒
    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

}
