package com.xiaosong.config.quartz;

import cn.hutool.crypto.SmUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.date.Constant;
import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.lib.ToolKits;
import com.dhnetsdk.module.LoginModule;
import com.sun.jna.ptr.IntByReference;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.wincc.visitor.VisService;
import com.xiaosong.common.wincc.failreceive.FailReceService;
import com.xiaosong.common.wincc.companyuser.StaffService;
import com.xiaosong.config.SendAccessRecord;
import com.xiaosong.constant.Constants;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbFailreceive;
import com.xiaosong.model.TbVisitor;
import com.xiaosong.util.DESUtil;
import com.xiaosong.util.Misc;
import com.xiaosong.util.NewWorldAuth;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 定时删除过期访客照片
 */
@DisallowConcurrentExecution
public class DelGoneVisitorRec implements Job {
    private VisService srvVisitor = VisService.me;  //访客业务层
    private StaffService srvStaff = StaffService.me;        //员工业务层
    private DeviceService srvDevice = DeviceService.me;     //设备业务层
    private SendAccessRecord sendAccessRecord = new SendAccessRecord(); //下发记录
    private FailReceService srvFail = FailReceService.me;     //下发失败业务层
    private static Logger logger = Logger.getLogger(DelGoneVisitorRec.class); //log日志
    private static String key = "iB4drRzSrC";

    /**
     * 删除三步：
     * 1，收集所有下发过人脸的设备IP
     * 2，收集不能删除人脸的设备IP（员工下发人脸的设备IP + 访客未过期的设备IP）
     * 3，取得需要删除人脸的设备IP
     */
    public void delGoneVisitor() throws Exception {

        List<TbVisitor> visitorList = srvVisitor.findByGoneDay();
        logger.info(visitorList.toString());

		/*
         * 	过期名单中找出同一人访问的数据
		 *
		 */

        for (TbVisitor visitor : visitorList) {
            //所有下发人脸的设备IP集合
            List<String> allAddr = new ArrayList<String>();
            //不可删除人脸的设备IP集合
            List<String> notGoneAddr = new ArrayList<>();
            // 删除的访客是否有员工身份
            TbCompanyuser visitorUser = srvStaff.findByNameAndIdNO(visitor.getVisitorName(),
                    visitor.getVisitorIdCard(), "normal");
                /*
				  *  有员工身份的访客，针对员工，notGoneAddr添加不能删除指定楼层的设备名单
				 */
            if (null != visitorUser) {
                logger.info(visitorUser.getUserName() + "有员工身份信息,所在通道" + visitorUser.getCompanyFloor());
                String visitorCompanyfloor = visitorUser.getCompanyFloor();
                // 员工照片所在的设备IP
                List<String> visitorUserDeviceIP = srvDevice.getAllFaceDeviceIP(visitorCompanyfloor);
                for (int i = 0; i < visitorUserDeviceIP.size(); i++) {
                    if (!allAddr.contains(visitorUserDeviceIP.get(i))) {
                        allAddr.add(visitorUserDeviceIP.get(i));
                    }
                    if (!notGoneAddr.contains(visitorUserDeviceIP.get(i))) {
                        notGoneAddr.add(visitorUserDeviceIP.get(i));
                    }
                }
            }
            //查找该访客所有未处理过名单的访问数据,（删除失败的+过期还未删除的）
            List<TbVisitor> visitorAllInfo = srvVisitor.findByVisitor(visitor.getVisitorName(),
                    visitor.getVisitorIdCard());
            for (int i = 0; i < visitorAllInfo.size(); i++) {
                // 查找该访客下发的所有设备IP（过期与未过期），通过被访者查找设备
                TbCompanyuser cUser = srvStaff.findByNameAndIdNO(
                        visitorAllInfo.get(i).getByVisitorName(), visitorAllInfo.get(i).getByVisitorIdCard(),
                        "normal");
                if(cUser==null){
                    logger.info("找不到被访者为"+ visitorAllInfo.get(i).getByVisitorName()+"的用户!");
                    return;
                }
                String cFloor = cUser.getCompanyFloor();
                List<String> cFaceIP = srvDevice.getAllFaceDeviceIP(cFloor);
                for (int j = 0; j < cFaceIP.size(); j++) {
                    if (!allAddr.contains(cFaceIP.get(j))) {
                        allAddr.add(cFaceIP.get(j));
                    }
                }
                // 查找该访客下发的所有设备IP（未过期）
                try {
                    if (Misc.compareDate(getDateTime2(), visitorAllInfo.get(i).getStartDateTime())
                            && Misc.compareDate(visitorAllInfo.get(i).getEndDateTime(), getDateTime2())) {
                        for (int j = 0; j < cFaceIP.size(); j++) {
                            if (!notGoneAddr.contains(cFaceIP.get(j))) {
                                notGoneAddr.add(cFaceIP.get(j));
                            }
                        }
                    }
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            //若不存在未过期数据，直接全部删除，不移除未过期数据
            if (notGoneAddr.size() > 0) {
                boolean removeSuc = allAddr.removeAll(notGoneAddr);
                logger.info("删除访客记录移除未过期数据集合结果:" + removeSuc);
                if (removeSuc == false) {
                    logger.info("访客集合移除错误");
                    return;
                }
            }
            //该访问记录是否已经执行过删除，如果执行过，删除失败表中的数据即可，否者正常删除
            if (visitor.getDelFlag().equals("1")) {
                if (allAddr.size() > 0) {
                    String delflag = "0";
                    for (int i = 0; i < allAddr.size(); i++) {
                        TbDevice device = srvDevice.findByDeviceIp(allAddr.get(i));
                        if (null == device) {
                            logger.error("设备表缺少IP为" + allAddr.get(i) + "的设备");
                            continue;
                        }
                        boolean isSuccess = true;
                        if (device.getDeviceType().equals("TPS980")) {
                            isSuccess = srvStaff.sendDelWhiteList(allAddr.get(i), visitor.getVisitorName(), visitor.getVisitorIdCard());
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            //linux下 初始化 海康sdk
//                        InitHCNetSDK.run(device.getDeviceType());
                            //winds下 初始化海康SDK
//                            devicesInit.initHC();
                            isSuccess = setUser(device, visitor);
                        } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                            if (null == visitor.getIdFrontImgUrl()) {
                                isSuccess = true;
                            } else {
//                                isSuccess = sendAccessRecord.delIPCpicture("service", visitor.getIdFrontImgUrl());
                            }
                        } else if (device.getDeviceType().equals("DH-ASI728")) {
                            isSuccess = deleteCard(device, visitor);
                        }else if(device.getDeviceType().equals("NL-RZ810")){
                            String number = DESUtil.decode(key,visitor.getVisitorIdCard());
                            String name = visitor.getVisitorName();

                            String type = "3";
                            String startDateTime = visitor.getStartDateTime();
                            String endDateTime = visitor.getEndDateTime();
                            long start = 0;
                            long end = 0;
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
                            try {
                                start = sdf.parse(startDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                                end = sdf.parse(endDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            String startTime = String.valueOf(start);
                            String endTime = String.valueOf(end);

                            String bid = visitor.getBid();
                            String rid = visitor.getRid();
                            JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "",type,startTime,endTime,bid,rid,device.getDeviceName());
                            if ("0".equals(jsonObject.getString("code"))) {
                                isSuccess=true;
                                String data1 = jsonObject.getString("data");
                                if (StringUtils.isNotBlank(data1)) {
                                    data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                    JSONObject value = JSON.parseObject(data1);
                                    System.out.println(data1);
                                    logger.info("data信息为{}" + value.toJSONString());
                                    //String bid = value.getString("bid");
                                    logger.info("服务端响应解密后数据：" + jsonObject);
                                }
                            } else {
                                isSuccess=false;
                                logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            }
                        }
                        if (isSuccess) {
                            logger.info("设备IP" + allAddr.get(i) + "删除" + visitor.getVisitorName() + "成功");
                        } else {
                            delflag = "1";
                            TbFailreceive one = srvFail.findOne(allAddr.get(i), visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor");
//                                TbFailreceive faceReceive = personController.getModel(TbFailreceive.class);                                faceReceive.setFaceIp(allAddr.get(i));
                            if(one==null){
                                TbFailreceive faceReceive = new TbFailreceive();
                                faceReceive.setFaceIp(allAddr.get(i));
                                faceReceive.setIdCard(visitor.getVisitorIdCard());
                                faceReceive.setUserName(visitor.getVisitorName());
                                faceReceive.setReceiveFlag("1");
                                faceReceive.setUserType("visitor");
                                faceReceive.setOpera("delete");
                                faceReceive.setDownNum(1);
                                faceReceive.setReceiveTime(getDateTime());
                                faceReceive.save();
                            }else{
                                int count = one.getDownNum();
                                count = count + 1;
                                one.setDownNum(count);
                                one.update();
                            }

                        }

                    }
                    visitor.setDelPosted("0");
                    visitor.setDelFlag(delflag);
                    visitor.update();
                }
            } else {
                //查询 删除 失败的 访客记录
                List<TbFailreceive> addrs = srvFail.findFaceIP(visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor");
                if (addrs.size() > 0) {
                    String delflag = "0";
                    for (int i = 0; i < addrs.size(); i++) {
                        TbDevice device = srvDevice.findByDeviceIp(addrs.get(i).getFaceIp());
                        if (null == device) {
                            logger.error("设备表缺少IP为" + allAddr.get(i) + "的设备");
                            continue;
                        }
                        boolean isSuccess = true;
                        if (device.getDeviceType().equals("TPS980")) {
                            isSuccess = srvStaff.sendDelWhiteList(addrs.get(i).getFaceIp(), visitor.getVisitorName(), visitor.getVisitorIdCard());
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            isSuccess = setUser(device, visitor);
                        } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                            if (null == visitor.getIdFrontImgUrl()) {
                                isSuccess = true;
                            } else {
//                                isSuccess = sendAccessRecord.delIPCpicture("service", visitor.getIdFrontImgUrl());
                            }
                        } else if (device.getDeviceType().equals("DH-ASI728")) {

                            isSuccess = deleteCard(device, visitor);
                        }else if(device.getDeviceType().equals("NL-RZ810")){
                            String number = DESUtil.decode(key,visitor.getVisitorIdCard());
                            String name = visitor.getVisitorName();

                            String type = "3";
                            String startDateTime = visitor.getStartDateTime();
                            String endDateTime = visitor.getEndDateTime();
                            long start = 0;
                            long end = 0;
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
                            try {
                                start = sdf.parse(startDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                                end = sdf.parse(endDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            String startTime = String.valueOf(start);
                            String endTime = String.valueOf(end);

                            String bid = visitor.getBid();
                            String rid = visitor.getRid();
                            JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "",type,startTime,endTime,bid,rid,device.getDeviceName());
                            if ("0".equals(jsonObject.getString("code"))) {
                                isSuccess=true;
                                String data1 = jsonObject.getString("data");
                                if (StringUtils.isNotBlank(data1)) {
                                    data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                    JSONObject value = JSON.parseObject(data1);
                                    System.out.println(data1);
                                    logger.info("data信息为{}" + value.toJSONString());
                                    //String bid = value.getString("bid");
                                    logger.info("服务端响应解密后数据：" + jsonObject);
                                }
                            } else {
                                isSuccess=false;
                                logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            }
                        }
                        //删除成功，将失败表中删除数据标准成功
                        if (isSuccess) {
                            logger.info("设备IP" + allAddr.get(i) + "删除" + visitor.getVisitorName() + "成功");
                            TbFailreceive tbFailreceive = addrs.get(i);
                            tbFailreceive.setReceiveFlag("0");
                            tbFailreceive.update();
                        } else {
                            delflag = "1";
                        }

                    }
                    visitor.setDelFlag(delflag);
                    visitor.update();
                }
            }

        }

        //redis统一删除
//        for (int i = 0; i < flagList.size(); i++) {
//            cache.del(flagList.get(i));
//        }
    }

    /**
     * 删除 大华 访客数据
     *
     * @param device
     * @param visitor
     * @return
     */
    private boolean deleteCard(TbDevice device, TbVisitor visitor) {
        //String strCardNo = "V" + visitor.getUserId();
        //登录
        LoginModule.login(device.getDeviceIp(), Constant.devicePort, Constants.deviceLoginName, Constants.deviceLoginPassWord);

        /**
         * 记录集操作
         */
        NetSDKLib.NET_CTRL_RECORDSET_PARAM msg = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
        msg.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
        msg.pBuf = new IntByReference(visitor.getCardInfo()).getPointer();

        msg.write();
        boolean bRet = LoginModule.netsdk.CLIENT_ControlDevice(LoginModule.m_hLoginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, msg.getPointer(), 5000);
        msg.read();

        if (!bRet) {
            logger.error("删除卡信息失败." + ToolKits.getErrorCodePrint());
            LoginModule.logout();
            return false;
        } else {
            logger.info("删除卡信息成功.");
        }
        //deleteFaceInfo(map);

        //退出登录
        LoginModule.logout();
        return true;
    }

    /**
     * 删除 海康设备的访客记录
     *
     * @param device  设备
     * @param visitor 访客数据
     * @return
     */
    private boolean setUser(TbDevice device, TbVisitor visitor) {
        String strCardNo = "V" + visitor.getUserId();
        boolean suc = false;

        try {
            if (!sendAccessRecord.delFace(device.getDeviceIp(), strCardNo)) {
                return suc;
            }
            suc = sendAccessRecord.delCard(device.getDeviceIp(), null, visitor);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return suc;
    }

    /**
     * 获取当前 时间 年月日 时分秒
     *
     * @return
     */
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 获取当前时间 年月日 时分
     *
     * @return
     */
    private String getDateTime2() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            delGoneVisitor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
