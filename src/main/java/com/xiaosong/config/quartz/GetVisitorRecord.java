package com.xiaosong.config.quartz;

import cn.hutool.crypto.SmUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.date.Constant;
import com.dhnetsdk.lib.NetSDKLib;
import com.dhnetsdk.lib.NetSDKLib.*;
import com.dhnetsdk.lib.ToolKits;
import com.dhnetsdk.module.LoginModule;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.sun.jna.Memory;
import com.xiaosong.common.Koala.StaffController;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.wincc.failreceive.FailReceService;
import com.xiaosong.common.wincc.companyuser.StaffService;
import com.xiaosong.common.wincc.visitor.VisService;
import com.xiaosong.config.SendAccessRecord;
import com.xiaosong.constant.Constants;
import com.xiaosong.constant.FaceDevResponse;
import com.xiaosong.model.*;
import com.xiaosong.util.*;
import com.xiaosong.util.QRCodeModel.GetStaffScheduleDataResponseModel;
import com.xiaosong.util.QRCodeModel.GetStaffScheduleResponseParentModel;
import com.xiaosong.util.QRCodeModel.GetStaffScheduleVisitorResponseModel;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 定时拉取访客数据
 */
//public class GetVisitorRecord implements Job {
@DisallowConcurrentExecution
public class GetVisitorRecord implements Job {

    private VisService srvVisitor = VisService.me;  //访客业务层
    private DeviceService srvDevice = DeviceService.me; //设备业务层
    private ServerService srvServer = ServerService.me; //服务器业务层
    private FailReceService srvFail = FailReceService.me; //下发失败业务层
    private StaffService srvStaff = StaffService.me; //员工业务层
    private SendAccessRecord sendAccessRecord = new SendAccessRecord();
    private static Logger logger = Logger.getLogger(GetVisitorRecord.class);
    private OkHttpUtil okHttpUtil = new OkHttpUtil();
    private Map<String, String> sendMap = new HashMap<>();
    private static String key = "iB4drRzSrC";
    public static String token = null;
    private Prop use = PropKit.use("config_product.properties");


//    @Override
//    public void execute(JobExecutionContext context) throws JobExecutionException {
//        try {
//            getStaff();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    public void getStaff() throws Exception {
        TbBuildingServer tbServerinfo = srvServer.findSer();

        if (!tbServerinfo.getOrgCode().isEmpty()) {
            // System.out.println("开始拉取数据");
            String towerNumber = tbServerinfo.getOrgCode();
            StringBuilder stringBuilder = new StringBuilder();
            // 拉取的数据地址
            //stringBuilder.append(Constants.baseURl);
            stringBuilder.append("http://" + tbServerinfo.getServerIp() + ":" + tbServerinfo.getServerPort() + "/visitor/");

            stringBuilder.append(Constants.newpullStaffUrl);
            stringBuilder.append("/");
            stringBuilder.append(tbServerinfo.getPospCode());
            stringBuilder.append("/");
            stringBuilder.append(towerNumber.trim());
            stringBuilder.append("/");
            stringBuilder.append(Constants.page);
            stringBuilder.append("/");
            stringBuilder.append(Constants.PAGENUMBER);
//            stringBuilder.append("?");
//            stringBuilder.append("companyId="+tbServerinfo.getIsFlagCompany());
//            stringBuilder.append("&");
//            stringBuilder.append("sectionId="+tbServerinfo.getSectionId());

            String url = stringBuilder.toString();
            logger.info("拉取地址：" + url);

            String responseContent = okHttpUtil.get(url);

            if (responseContent.isEmpty()) {
                logger.info("没获取到访问数据");
//                String keysign = towerInforService.findOrgId()+towerInforService.findPospCode()+towerInforService.findKey();
                //logger.sendErrorLog(towerInforService.findOrgId(), "没获取到访客访问的数据", "","数据错误", Constants.errorLogUrl,keysign);
                return;
            }

            // 返回数据转成json模式
            GetStaffScheduleResponseParentModel getStaffScheduleResponseParentModel = JSON.parseObject(responseContent,
                    GetStaffScheduleResponseParentModel.class);
            if (getStaffScheduleResponseParentModel == null) {
                logger.error("返回数据格式不正确");
                //String keysign = towerInforService.findOrgId()+towerInforService.findPospCode()+towerInforService.findKey();
                //logger.sendErrorLog(towerInforService.findOrgId(), "访客访问的返回数据格式不正确", "","数据错误", Constants.errorLogUrl,keysign);
                return;
            }
            List<TbVisitor> staffList = srvVisitor.findByIssued("1");
            // 获取其数据
            GetStaffScheduleDataResponseModel getStaffScheduleDataResponseModel = getStaffScheduleResponseParentModel
                    .getData();

            if (getStaffScheduleDataResponseModel == null) {
                logger.warn("无访问数据内容");
            } else {
                List<GetStaffScheduleVisitorResponseModel> getStaffScheduleVisitorResponseModels = getStaffScheduleDataResponseModel
                        .getRows();

                if ((getStaffScheduleVisitorResponseModels == null)
                        || (getStaffScheduleVisitorResponseModels.size() <= 0)) {
                    logger.warn("无访问数据内容");
                } else {
                    for (GetStaffScheduleVisitorResponseModel getStaffScheduleVisitorResponseModel : getStaffScheduleVisitorResponseModels) {
                        TbVisitor tbStaff = new TbVisitor();
                        tbStaff.setCity(getStaffScheduleVisitorResponseModel.getCity());
                        tbStaff.setEndDateTime(getStaffScheduleVisitorResponseModel.getEndDate().substring(0, 16));
                        tbStaff.setOrgCode(getStaffScheduleVisitorResponseModel.getOrgCode());
                        tbStaff.setProvince(getStaffScheduleVisitorResponseModel.getProvince());
                        tbStaff.setSoleCode(getStaffScheduleVisitorResponseModel.getSoleCode());
                        tbStaff.setStartDateTime(getStaffScheduleVisitorResponseModel.getStartDate().substring(0, 16));
                        //访问时间提前半小时有效
                        String preTime = preStartTime(getStaffScheduleVisitorResponseModel.getStartDate(), -30);
                        tbStaff.setPreStartTime(preTime);
                        tbStaff.setVisitorName(getStaffScheduleVisitorResponseModel.getUserRealName());
                        tbStaff.setVisitDate(getStaffScheduleVisitorResponseModel.getVisitDate());
                        tbStaff.setVisitTime(getStaffScheduleVisitorResponseModel.getVisitTime());
                        tbStaff.setByVisitorName(getStaffScheduleVisitorResponseModel.getVistorRealName());
//                      tbStaff.setDatetype(getStaffScheduleVisitorResponseModel.getDateType());
                        tbStaff.setUserId(getStaffScheduleVisitorResponseModel.getUserId());
                        tbStaff.setDelFlag("1");
                        tbStaff.setDelPosted("1");
                        if (getStaffScheduleVisitorResponseModel.getUserIdNO() == null) {
                            tbStaff.setIsSued("0");
                        } else {
                            tbStaff.setIsSued("1");
                        }
                        tbStaff.setIsPosted("0");
                        tbStaff.setVisitId(getStaffScheduleVisitorResponseModel.getVisitId());
                        tbStaff.setVisitorIdCard(getStaffScheduleVisitorResponseModel.getUserIdNO());
                        tbStaff.setPhoto(getStaffScheduleVisitorResponseModel.getPhoto());
                        tbStaff.setByVisitorIdCard(getStaffScheduleVisitorResponseModel.getVisitorIdNO());
                        tbStaff.setBid(getStaffScheduleVisitorResponseModel.getBid());
                        String uIdStaffId = UUID.randomUUID().toString().replaceAll("\\-", "");
                        tbStaff.setId(uIdStaffId);

                        // 存数据

                        // 向云端确认存储
                        confirmReceiveData(towerNumber, tbStaff.getVisitId());
                        if (getStaffScheduleVisitorResponseModel.getPhoto() != null) {
                            String fileName = null;
                            byte[] photoKey = Base64_2.decode(getStaffScheduleVisitorResponseModel.getPhoto());
                            fileName = getStaffScheduleVisitorResponseModel.getUserRealName()
                                    + getStaffScheduleVisitorResponseModel.getVisitId() + ".jpg";
                            File file = FilesUtils.getFileFromBytes(photoKey, Constants.VisitorPath, fileName);

                            File reduceFile = new File(Constants.VisitorPath + fileName);
                            String name = reduceFile.getName();
                            String absolutePath = reduceFile.getAbsolutePath();
                            String path = reduceFile.getParentFile().getPath();

                            String b = Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024 * 90L));
                            byte[] photoKey1 = Base64_2.decode(b);
                            FilesUtils.getFileFromBytes(photoKey1, Constants.VisitorPath, name);

                            tbStaff.setPhoto(file.getAbsolutePath());
                            tbStaff.save();

                            //FilesUtils.getFileFromBytes(photoKey, "E:\\sts-space\\photoCache\\service", fileName);
                        } else {
                            logger.error(getStaffScheduleVisitorResponseModel.getUserRealName() + "该用户无照片");
                            continue;
                        }
                        staffList.add(tbStaff);

                    }
                }
            }
            // 数据分析

            if (staffList.size() == 0 || staffList == null) {
                logger.warn("无访客下发数据");
                return;
            }
            sendFaceData(staffList);
        }

    }

    private void confirmReceiveData(String towerNumber, String visitId) throws Exception {

        StringBuilder stringBuilder = new StringBuilder();
        //stringBuilder.append(Constants.baseURl);
        TbBuildingServer tbServerinfo = srvServer.findSer();
        stringBuilder.append("http://" + tbServerinfo.getServerIp() + ":" + tbServerinfo.getServerPort() + "/visitor/");

        stringBuilder.append(Constants.newconfirmReceiveUrl);
        stringBuilder.append("/");
        stringBuilder.append(tbServerinfo.getPospCode());
        stringBuilder.append("/");
        stringBuilder.append(towerNumber);
        stringBuilder.append("/");
        stringBuilder.append(visitId);
//        stringBuilder.append("?");
//        stringBuilder.append("companyId="+tbServerinfo.getIsFlagCompany());
//        stringBuilder.append("&");
//        stringBuilder.append("sectionId="+tbServerinfo.getSectionId());

        String url = stringBuilder.toString();
        String responseContent = okHttpUtil.get(url);
        logger.info(responseContent);
    }

    private void sendFaceData(List<TbVisitor> visitors) throws Exception {
        // TODO Auto-generated method stub

        // 访客下发名单
        for (TbVisitor visitor : visitors) {

            // 该访客是否还有员工身份
            TbCompanyuser companyUser = srvStaff.findByNameAndIdNO(visitor.getVisitorName(), visitor.getVisitorIdCard(), "normal");
            if (null == companyUser) {
                logger.info("正常下发访客");
                // 正常下发
                if (visitor.getIsPosted().equals("1")) {
                    //失败数据再下发
                    doPosted(visitor);
                } else {
                    //新数据下发
                    doFirstPost(visitor);
                }
            } else {
                // 访客通过被访者所在楼层将访客信息下发到指定设备
                TbCompanyuser interviewee = srvStaff.findByNameAndIdNO(visitor.getByVisitorName(), visitor.getByVisitorIdCard(), "normal");
                if (null == interviewee) {
                    continue;
                }
                String floor = interviewee.getCompanyFloor();
                // 被访者相关联设备,即访客需要下发的设备
                List<String> allinterDecive = srvDevice.getAllFaceDeviceIP(floor);
                // 访客的员工身份相关联设备
                List<String> allStaffDecive = srvDevice.getAllFaceDeviceIP(companyUser.getCompanyFloor());
                for (int i = 0; i < allinterDecive.size(); i++) {
                    //访客员工身份的相关联设备是否与访问楼层相关联的设备重叠
                    if (allStaffDecive.contains(allinterDecive.get(i))) {
                        if (companyUser.getIsSued().equals("1")) {
                            //失败记录表查找该员工下发失败的设备
                            List<TbFailreceive> failRecord = srvFail.findByNameAndid(companyUser.getUserName(), companyUser.getIdNO(), "normal");
                            if (null == failRecord) {
                                //
                                logger.info("失败记录表无访客的员工身份信息");
                                continue;
                            } else {
                                for (TbFailreceive faileData : failRecord) {
                                    if (faileData.getFaceIp().equals(allinterDecive.get(i))) {
                                        //指定员工下发指定设备
                                        logger.info(allinterDecive.get(i) + "接收访客信息");
                                        sendPointDevice(visitor, allinterDecive.get(i));
                                    } else {
                                        continue;
                                    }
                                }
                            }
                        } else {
                            Db.update("UPDATE tb_visitor SET isSued='0' ,isPosted='1' WHERE visitorName = ? and preStartTime = ?", visitor.getVisitorName(),visitor.getPreStartTime());

                            continue;
                        }
                    } else {
                        //指定员工下发指定设备
                        sendPointDevice(visitor, allinterDecive.get(i));
                    }
                }

            }

        }

    }

    //指定一台设备下发
    private void sendPointDevice(TbVisitor vistor, String deviceIp) throws Exception {
        String idNO = vistor.getByVisitorIdCard();
        String visitorname = vistor.getByVisitorName();

        if (idNO == null || visitorname == null) {
            logger.warn("被访人证件号或者姓名为空，找不到该员工数据");
            return;
        } else {
            TbCompanyuser companyUser = srvStaff.findByNameAndIdNO(visitorname, idNO, "normal");

            if (null == companyUser) {
                return;
            }

            String photo = isPhoto(vistor);
            if (photo == null) {
                return;
            }
            if (null != deviceIp) {
                logger.info("需下发的人像识别仪器IP为：" + deviceIp);

                TbDevice device = srvDevice.findByDeviceIp(deviceIp);
                if (null == device) {
                    logger.error("设备表缺少IP为" + deviceIp + "的设备");
                    return;
                }
                boolean isSuccess = true;
                boolean isSuccess2 = true;
                if (device.getDeviceType().equals("TPS980")) {
                    isSuccess = this.sendWhiteList(deviceIp, vistor, photo);
                } else if (device.getDeviceType().equals("DS-K5671")) {

                    isSuccess = sendAccessRecord.setCardAndFace(deviceIp, null, vistor);

                } else if (device.getDeviceType().equals("DS-K5671-H")) {
                    isSuccess = sendAccessRecord.setCardAndFace(deviceIp, null, vistor);
                    isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, Integer.parseInt(vistor.getUserId()), vistor.getVisitorName(), "V" + vistor.getUserId(), vistor.getStartDateTime(), vistor.getEndDateTime());

                } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                    File picAppendData = IPCxmlFile(vistor);
                    String filePath = Constants.VisitorPath + "/" + vistor.getVisitorName() + vistor.getVisitId() + ".jpg";
                    File picture = new File(filePath);
//                    isSuccess = sendAccessRecord.sendToIPC(deviceIp, picture, picAppendData, null, vistor,  device.getAdmin(), device.getPassword());
                } else if (device.getDeviceType().equals("DH-ASI728")) {

                    sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                    sendMap.put(Constant.photoPath, photo);      //图片*
                    sendMap.put(Constant.userId, String.valueOf(vistor.getUserId()));       //用户id
                    sendMap.put(Constant.username, vistor.getVisitorName());   //用户姓名*
//                    sendMap.put(Constants.idNo, vistor.getVisitorIdCard());     //身份证号码
                    int cardInfo=insertInfo(sendMap,vistor);
                    if(cardInfo==0){
                        isSuccess=false;
                    }else{
                        vistor.setCardInfo(cardInfo);
                        vistor.update();
                    }
                } else if (device.getDeviceType().equals("NL-RZ810")) {
                    String number = DESUtil.decode(key, vistor.getVisitorIdCard());
                    String name = vistor.getVisitorName();
                    String type = "1";
                    String startDateTime = vistor.getStartDateTime();
                    String endDateTime = vistor.getEndDateTime();
                    long start = 0;
                    long end = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
                    try {
                        start = sdf.parse(startDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                        end = sdf.parse(endDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String startTime = String.valueOf(start);
                    String endTime = String.valueOf(end);
                    String bid = vistor.getBid();
                    JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "", type, startTime, endTime, bid, "", device.getDeviceName());
                    if ("0".equals(jsonObject.getString("code"))) {
                        isSuccess = true;
                        String data1 = jsonObject.getString("data");
                        if (StringUtils.isNotBlank(data1)) {
                            data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                            JSONObject value = JSON.parseObject(data1);
                            logger.warn(data1);
                            logger.info("data信息为{}" + value.toJSONString());
                            String rid = value.getString("rid");
                            logger.info("服务端响应解密后数据：" + jsonObject);
                            vistor.setRid(rid);
                            vistor.update();
                        }
                    } else {
                        logger.info("失败原因：{}" + jsonObject.getString("msg"));
                        isSuccess = false;
                    }
                }else if (device.getDeviceType().equals("KS-250")) {
                    if (token == null) {
                        login();
                    }
                    String url = use.get("url");
                    String path = "http://" + url + ":80/subject/file";

                    String start = String.valueOf(getTimeStamp(vistor.getStartDateTime()) / 1000);
                    String end = String.valueOf(getTimeStamp(vistor.getEndDateTime()) / 1000);

                    String str = StaffController.doPost(path, vistor.getVisitorName(), "1", photo, start, end,token);


                    JSONObject parse = JSONObject.parseObject(str);

                    String data = parse.getString("data");
                    JSONObject par = JSONObject.parseObject(data);
                    String id = par.getString("id");

                    Integer code = (Integer) parse.get("code");
                    if(code==0){
                        isSuccess = true;
                        vistor.setUserId(id);
                    }else{
                        isSuccess = false;
                    }
                }
                logger.info("下发数据结果" + isSuccess);
                if (vistor.getIsPosted().equals("0")) {
                    String issued = "0";
                    if (!isSuccess || !isSuccess2) {
                        issued = "1";
                        TbFailreceive faceReceive = srvFail.findOne(deviceIp, vistor.getVisitorName(), vistor.getVisitorIdCard(), "visitor");
                        if (null == faceReceive) {
                            TbFailreceive newFaceFail = new TbFailreceive();
                            newFaceFail.setFaceIp(deviceIp);
                            newFaceFail.setIdCard(vistor.getVisitorIdCard());
                            newFaceFail.setUserName(vistor.getVisitorName());
                            newFaceFail.setReceiveFlag("1");
                            newFaceFail.setUserType("visitor");
                            newFaceFail.setDownNum(0);
                            newFaceFail.setOpera("save");
                            newFaceFail.setReceiveTime(getDateTime());
                            newFaceFail.setVisitorUUID(vistor.getId());
                            newFaceFail.save();
                        } else {
                            int count = faceReceive.getDownNum();
                            count++;
                            faceReceive.setDownNum(count);
                            faceReceive.update();
                        }
                    }

                    Db.update("UPDATE tb_visitor SET isSued=? ,isPosted='1' WHERE visitorName = ? and preStartTime = ?", issued, vistor.getVisitorName(),vistor.getPreStartTime());

                } else {
                    String issued = "0";
                    TbFailreceive facefail = srvFail.findByVisitorUUId(vistor.getId());
                    if (!isSuccess || !isSuccess2) {
                        issued = "1";
                    } else {
                        issued = "0";
                        facefail.setReceiveFlag("0");
                        facefail.update();
                    }
                    Db.update("UPDATE tb_visitor SET isSued=? ,isPosted='1' WHERE visitorName = ? and preStartTime = ?", issued, vistor.getVisitorName(),vistor.getPreStartTime());

                }

            }
        }
    }

    /**
     * 新数据下发
     *
     * @param visitor 访客数据
     * @throws Exception
     */
    private void doFirstPost(TbVisitor visitor) throws Exception {

        String idNO = visitor.getByVisitorIdCard();
        String visitorname = visitor.getByVisitorName();

        if (idNO == null || visitorname == null) {
            logger.warn("被访人证件号或者姓名为空，找不到该员工数据");
            return;
        } else {
            TbCompanyuser companyUser = srvStaff.findByNameAndIdNO(visitorname, idNO, "normal");

            if (null == companyUser) {
                return;
            }

            String companyfloor = null;
            if (null != companyUser.getCompanyFloor()) {
                companyfloor = companyUser.getCompanyFloor();
            }
            List<String> allFaceDecive = srvDevice.getAllFaceDeviceIP(companyfloor);
            String photo = isPhoto(visitor);
            if (photo == null) {
                return;
            }
            if (allFaceDecive.size() > 0) {
                String issued = "0";
                for (int i = 0; i < allFaceDecive.size(); i++) {
                    logger.info("需下发的人像识别仪器IP为：" + allFaceDecive.get(i));
                    TbDevice device = srvDevice.findByDeviceIp(allFaceDecive.get(i));
                    if (null == device) {
                        logger.error("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                        continue;
                    }
                    boolean isSuccess = true;
                    boolean isSuccess2 = true;
                    if (device.getDeviceType().equals("TPS980")) {
                        isSuccess = this.sendWhiteList((String) allFaceDecive.get(i), visitor, photo);
                    } else if (device.getDeviceType().equals("DS-K5671")) {

                        isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), null, visitor);

                    } else if (device.getDeviceType().equals("DS-K5671-H")) {
                        isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), null, visitor);
                        isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, Integer.parseInt(visitor.getUserId()), visitor.getVisitorName(), "V" + visitor.getUserId(), visitor.getStartDateTime(), visitor.getEndDateTime());

                    } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                        File picAppendData = IPCxmlFile(visitor);
                        String filePath = Constants.VisitorPath + "/" + visitor.getVisitorName() + visitor.getVisitId() + ".jpg";
                        File picture = new File(filePath);
//                        isSuccess = sendAccessRecord.sendToIPC((String) allFaceDecive.get(i), picture, picAppendData, null, visitor,  device.getAdmin(), device.getPassword());
                    } else if (device.getDeviceType().equals("DH-ASI728")) {

                        sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                        sendMap.put(Constant.photoPath, photo);      //图片*
                        sendMap.put(Constant.userId, String.valueOf(visitor.getUserId()));       //用户id
                        sendMap.put(Constant.username, visitor.getVisitorName());   //用户姓名*
                        //sendMap.put(Constants.idNo, visitor.getVisitorIdCard());     //身份证号码
                        int cardInfo=insertInfo(sendMap,visitor);
                        if(cardInfo==0){
                            isSuccess=false;
                        }else{
                            visitor.setCardInfo(cardInfo);
                            visitor.update();
                        }

                    } else if (device.getDeviceType().equals("NL-RZ810")) {
                        String number = DESUtil.decode(key, visitor.getVisitorIdCard());
                        String name = visitor.getVisitorName();
                        String type = "1";
                        String startDateTime = visitor.getStartDateTime();
                        String endDateTime = visitor.getEndDateTime();
                        long start = 0;
                        long end = 0;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
                        try {
                            start = sdf.parse(startDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                            end = sdf.parse(endDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String startTime = String.valueOf(start);
                        String endTime = String.valueOf(end);
                        String bid = visitor.getBid();
                        JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "", type, startTime, endTime, bid, "", device.getDeviceName());
                        if ("0".equals(jsonObject.getString("code"))) {
                            isSuccess = true;
                            String data1 = jsonObject.getString("data");
                            if (StringUtils.isNotBlank(data1)) {
                                data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                JSONObject value = JSON.parseObject(data1);
                                logger.warn(data1);
                                logger.info("data信息为{}" + value.toJSONString());
                                String rid = value.getString("rid");
                                logger.info("服务端响应解密后数据：" + jsonObject);
                                visitor.setRid(rid);
                                visitor.update();
                            }
                        } else {
                            logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            isSuccess = false;
                        }
                    } else if (device.getDeviceType().equals("KS-250")) {
                        if (token == null) {
                            login();
                        }
                        String url = use.get("url");
                        String path = "http://" + url + ":80/subject/file";

                        String start = String.valueOf(getTimeStamp(visitor.getStartDateTime()) / 1000);
                        String end = String.valueOf(getTimeStamp(visitor.getEndDateTime()) / 1000);

                        String str = StaffController.doPost(path, visitor.getVisitorName(), "1", photo, start, end,token);


                        JSONObject parse = JSONObject.parseObject(str);

                        String data = parse.getString("data");
                        JSONObject par = JSONObject.parseObject(data);
                        String id = par.getString("id");

                        Integer code = (Integer) parse.get("code");
                        if(code==0){
                            isSuccess = true;
                            visitor.setUserId(id);
                        }else{
                            isSuccess = false;
                        }
                    }
                    visitor.setIsPosted("1");
                    logger.info("下发数据结果" + isSuccess);
                    if (!isSuccess || !isSuccess2) {
                        issued = "1";
                        TbFailreceive faceReceive = srvFail.findOne(allFaceDecive.get(i), visitor.getVisitorName(), visitor.getVisitorIdCard(), "visitor");
                        if (null == faceReceive) {
                            TbFailreceive newFaceFail = new TbFailreceive();
                            newFaceFail.setFaceIp(allFaceDecive.get(i));
                            newFaceFail.setIdCard(visitor.getVisitorIdCard());
                            newFaceFail.setUserName(visitor.getVisitorName());
                            newFaceFail.setReceiveFlag("1");
                            newFaceFail.setUserType("visitor");
                            newFaceFail.setDownNum(0);
                            newFaceFail.setOpera("save");
                            newFaceFail.setReceiveTime(getDateTime());
                            newFaceFail.setVisitorUUID(visitor.getId());
                            newFaceFail.save();
                        } else {
                            int count = faceReceive.getDownNum();
                            count++;
                            faceReceive.setDownNum(count);
                            faceReceive.update();
                        }
                    }
                    continue;
                }
                Db.update("UPDATE tb_visitor SET isSued=? WHERE visitorName = ? and preStartTime = ?", issued, visitor.getVisitorName(),visitor.getPreStartTime());
            }
            return;
        }

    }

    /**
     * 失败数据再下发
     *
     * @param vistor 访客数据
     * @throws Exception
     */
    private void doPosted(TbVisitor vistor) throws Exception {

        if (vistor.getIsSued().equals("0")) {
            return;
        } else {
            List<TbFailreceive> faceReceiveList = srvFail.findByFaceFlag("1", "visitor");
            if (faceReceiveList.size() <= 0) {
                return;
            } else {
                String issued = "0";
                for (TbFailreceive faceReceive : faceReceiveList) {

                    TbVisitor tbStaff = srvVisitor.findByUUID(faceReceive.getVisitorUUID());
                    if (null == tbStaff) {
                        continue;
                    }
                    String photo = isPhoto(tbStaff);
                    if (photo == null) {
                        logger.error("缺失照片");
                        continue;
                    }
                    TbDevice device = srvDevice.findByDeviceIp(faceReceive.getFaceIp());
                    if (null == device) {
                        logger.error("设备表缺少IP为" + faceReceive.getFaceIp() + "的设备");
                        return;
                    }
                    boolean isSuccess = true;
                    boolean isSuccess2 = true;
                    if (device.getDeviceType().equals("TPS980")) {
                        isSuccess = this.sendWhiteList(faceReceive.getFaceIp(), tbStaff, photo);
                    } else if (device.getDeviceType().equals("DS-K5671")) {

                        isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), null, vistor);

                    } else if (device.getDeviceType().equals("DS-K5671-H")) {
                        isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), null, vistor);
                        isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, Integer.parseInt(vistor.getUserId()), vistor.getVisitorName(), "V" + vistor.getUserId(), vistor.getStartDateTime(), vistor.getEndDateTime());

                    } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                        File picAppendData = IPCxmlFile(vistor);
                        String filePath = Constants.VisitorPath + "/" + vistor.getVisitorName() + vistor.getVisitId() + ".jpg";
                        File picture = new File(filePath);
//                        isSuccess = sendAccessRecord.sendToIPC(faceReceive.getFaceIp(), picture, picAppendData, null, vistor,  device.getAdmin(), device.getPassword());
                    } else if (device.getDeviceType().equals("DH-ASI728")) {

                        sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                        sendMap.put(Constant.photoPath, photo);      //图片*
                        sendMap.put(Constant.userId, String.valueOf(vistor.getUserId()));       //用户id
                        sendMap.put(Constant.username, vistor.getVisitorName());   //用户姓名*
                        //sendMap.put(Constants.idNo, "idNo");           //身份证号码
                        int cardInfo=insertInfo(sendMap,vistor);
                        if(cardInfo==0){
                            isSuccess=false;
                        }else{
                            vistor.setCardInfo(cardInfo);
                            vistor.update();
                        }

                    } else if (device.getDeviceType().equals("NL-RZ810")) {
                        String number = DESUtil.decode(key, vistor.getVisitorIdCard());
                        String name = vistor.getVisitorName();
                        String type = "1";
                        String startDateTime = vistor.getStartDateTime();
                        String endDateTime = vistor.getEndDateTime();
                        long start = 0;
                        long end = 0;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");//要转换的日期格式，根据实际调整""里面内容
                        try {
                            start = sdf.parse(startDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                            end = sdf.parse(endDateTime).getTime();//sdf.parse()实现日期转换为Date格式，然后getTime()转换为毫秒数值
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String startTime = String.valueOf(start);
                        String endTime = String.valueOf(end);
                        String bid = vistor.getBid();
                        JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "", type, startTime, endTime, bid, "", device.getDeviceName());
                        if ("0".equals(jsonObject.getString("code"))) {
                            isSuccess = true;
                            String data1 = jsonObject.getString("data");
                            if (StringUtils.isNotBlank(data1)) {
                                data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                JSONObject value = JSON.parseObject(data1);
                                logger.warn(data1);
                                logger.info("data信息为{}" + value.toJSONString());
                                String rid = value.getString("rid");
                                logger.info("服务端响应解密后数据：" + jsonObject);
                                vistor.setRid(rid);
                                vistor.update();
                            }
                        } else {
                            logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            isSuccess = false;
                        }
                    }else if (device.getDeviceType().equals("KS-250")) {
                        if (token == null) {
                            login();
                        }
                        String url = use.get("url");
                        String path = "http://" + url + ":80/subject/file";

                        String start = String.valueOf(getTimeStamp(vistor.getStartDateTime()) / 1000);
                        String end = String.valueOf(getTimeStamp(vistor.getEndDateTime()) / 1000);

                        String str = StaffController.doPost(path, vistor.getVisitorName(), "1", photo, start, end,token);


                        JSONObject parse = JSONObject.parseObject(str);

                        String data = parse.getString("data");
                        JSONObject par = JSONObject.parseObject(data);
                        String id = par.getString("id");

                        Integer code = (Integer) parse.get("code");
                        if(code==0){
                            isSuccess = true;
                            vistor.setUserId(id);
                        }else{
                            isSuccess = false;
                        }
                    }
                    //boolean isSuccess =false;
                    if (!isSuccess || !isSuccess2) {
                        issued = "1";
                        logger.error("失败名单下发" + tbStaff.getVisitorName() + "再次失败");
                    } else {
                        faceReceive.setReceiveFlag("0");
                        faceReceive.update();
                    }
                }
                Db.update("UPDATE tb_visitor SET isSued=? WHERE visitorName = ? and preStartTime = ?", issued, vistor.getVisitorName(),vistor.getPreStartTime());
            }
            return;
        }

    }

    /**
     * 照片
     *
     * @param vistor 访客数据
     * @return
     * @throws Exception
     */
    private String isPhoto(TbVisitor vistor) throws Exception {
        String filePath = Constants.VisitorPath + vistor.getVisitorName() + vistor.getVisitId() + ".jpg";
        //Db.update("UPDATE tb_visitor SET photo=? WHERE visitorName = ? ", filePath, vistor.getVisitorName());
        System.out.println("照片路劲:" + filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error(vistor.getVisitorName() + "无照片");
            return null;
        }
        return filePath;
    }

    /**
     * 海景 访客数据下发
     *
     * @param deviceIp 设备ip
     * @param visitor  访客数据
     * @param photo    照片
     * @return
     * @throws Exception
     */
    private boolean sendWhiteList(String deviceIp, TbVisitor visitor, String photo) throws Exception {

        JSONObject paramsJson = new JSONObject();
        String URL = "http://" + deviceIp + ":8080/office/addOrDelUser";
        // String option = user.getCurrentStatus().equals("normal") ? "save" : "delete";
        // System.out.println(service.getUserrealname()+"++"+service.getIdNO());
        paramsJson.put("name", visitor.getVisitorName());
        paramsJson.put("idCard", visitor.getVisitorIdCard());
        paramsJson.put("op", "save");
        paramsJson.put("type", "staff");
        byte[] bytesFromFile = FilesUtils.getBytesFromFile(new File(photo));
        paramsJson.put("imageFile", bytesFromFile);

        StringEntity entity = new StringEntity(paramsJson.toJSONString(), "UTF-8");
        ThirdResponseObj thirdResponseObj = null;
        entity.setContentType("aaplication/json");
        try {
            thirdResponseObj = HttpUtil.http2Se(URL, entity, "UTF-8");
        } catch (Exception e) {
            logger.error("访客数据下发设备" + deviceIp + "错误-->" + e.getMessage());
            return false;
        }
        if (thirdResponseObj == null) {
            logger.error("人脸识别仪器" + deviceIp + "接收" + visitor.getVisitorName() + "失败,");
            return false;
        }

        FaceDevResponse faceResponse = JSON.parseObject(thirdResponseObj.getResponseEntity(), FaceDevResponse.class);

        if ("success".equals(thirdResponseObj.getCode())) {
            logger.info(visitor.getVisitorName() + "下发" + deviceIp + "成功");
        } else {
            logger.error(visitor.getVisitorName() + "下发" + deviceIp + "失败");
            return false;
        }
        if ("001".equals(faceResponse.getResult())) {
            logger.info("人脸设备接收" + visitor.getVisitorName() + "成功");
            return true;
        } else {
            logger.error("人脸设备接收" + visitor.getVisitorName() + "失败，失败原因：" + faceResponse.getMessage());
            return false;
        }

    }


    /**
     * 获取当前时间 年月日,时分秒
     *
     * @return
     */
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 获取当前时间 年月日, 时分
     *
     * @param dateTime
     * @param pretime
     * @return
     * @throws Exception
     */
    private String preStartTime(String dateTime, int pretime) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date dt = sdf.parse(dateTime);
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(dt);
        rightNow.add(Calendar.MINUTE, pretime);
        return sdf.format(rightNow.getTime());
    }

    /**
     * 海康摄像头 照片
     *
     * @param visitor 访客数据
     * @return
     */
    public File IPCxmlFile(TbVisitor visitor) {
        // TODO Auto-generated method stub
        String filePath = Constants.VisitorPath + "/" + visitor.getVisitorName() + visitor.getUserId() + ".xml";
        File filepath = new File(Constants.VisitorPath);
        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        File file = new File(filePath);

        StringBuilder builder = new StringBuilder();
        builder.append("<FaceAppendData><name>V");
        builder.append(visitor.getVisitorName());
        builder.append("</name><certificateType>ID</certificateType><certificateNumber>");
        builder.append(visitor.getUserId());
        builder.append("</certificateNumber></FaceAppendData>");

        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8");
            StringBuilder outputString = new StringBuilder();
            outputString.append(builder.toString());
            out.write(outputString.toString());

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 大华添加设备卡信息
     *
     * @param map 添加参数
     * @param vistor
     * @return true:成功   false:失败
     */
    public int insertInfo(Map<String, String> map, TbVisitor vistor) {
        //登录
        LoginModule.login(map.get(Constant.deviceIp), Constant.devicePort, Constants.deviceLoginName, Constants.deviceLoginPassWord);

        /**
         * 门禁卡记录集信息
         */
        NET_RECORDSET_ACCESS_CTL_CARD accessCardInfo = new NET_RECORDSET_ACCESS_CTL_CARD();

        // 卡号
        String cardNo = "V" + map.get(Constant.userId);
        System.arraycopy(cardNo.getBytes(), 0, accessCardInfo.szCardNo, 0, cardNo.getBytes().length);

        // 用户ID
        System.arraycopy(map.get(Constant.userId).getBytes(), 0, accessCardInfo.szUserID, 0, map.get(Constant.userId).getBytes().length);

        // 卡名(设备上显示的姓名)
        try {
            System.arraycopy(map.get(Constant.username).getBytes("GBK"), 0, accessCardInfo.szCardName, 0, map.get(Constant.username).getBytes("GBK").length);
            //System.arraycopy(NameUtils.AccordingToName(map.get(Constant.username)).getBytes("GBK"), 0, accessCardInfo.szCardName, 0, map.get(Constant.username).getBytes("GBK").length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 卡密码
        System.arraycopy("0000".getBytes(), 0, accessCardInfo.szPsw, 0, "0000".getBytes().length);

        //-- 设置开门权限
        accessCardInfo.nDoorNum = 2;
        accessCardInfo.sznDoors[0] = 0;
        accessCardInfo.sznDoors[1] = 1;
        accessCardInfo.nTimeSectionNum = 2;      // 与门数对应
        accessCardInfo.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
        accessCardInfo.sznTimeSectionNo[1] = 255; // 表示第二个门全天有效

        // 有效开始时间
        String[] startTimes = vistor.getStartDateTime().split(" ");
        accessCardInfo.stuValidStartTime.dwYear = Integer.parseInt(startTimes[0].split("-")[0]);
        accessCardInfo.stuValidStartTime.dwMonth = Integer.parseInt(startTimes[0].split("-")[1]);
        accessCardInfo.stuValidStartTime.dwDay = Integer.parseInt(startTimes[0].split("-")[2]);
        accessCardInfo.stuValidStartTime.dwHour = Integer.parseInt(startTimes[1].split(":")[0]);
        accessCardInfo.stuValidStartTime.dwMinute = Integer.parseInt(startTimes[1].split(":")[1]);
        accessCardInfo.stuValidStartTime.dwSecond = Integer.parseInt("00");

        // 有效结束时间
        String[] endTimes = vistor.getEndDateTime().split(" ");
        accessCardInfo.stuValidEndTime.dwYear = Integer.parseInt(endTimes[0].split("-")[0]);
        accessCardInfo.stuValidEndTime.dwMonth = Integer.parseInt(endTimes[0].split("-")[1]);
        accessCardInfo.stuValidEndTime.dwDay = Integer.parseInt(endTimes[0].split("-")[2]);
        accessCardInfo.stuValidEndTime.dwHour = Integer.parseInt(endTimes[1].split(":")[0]);
        accessCardInfo.stuValidEndTime.dwMinute = Integer.parseInt(endTimes[1].split(":")[1]);
        accessCardInfo.stuValidEndTime.dwSecond = Integer.parseInt("00");

        /**
         * 记录集操作
         */
        NetSDKLib.NET_CTRL_RECORDSET_INSERT_PARAM insert = new NetSDKLib.NET_CTRL_RECORDSET_INSERT_PARAM();
        insert.stuCtrlRecordSetInfo.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;   // 记录集类型
        insert.stuCtrlRecordSetInfo.pBuf = accessCardInfo.getPointer();

        accessCardInfo.write();
        insert.write();
        boolean bRet = LoginModule.netsdk.CLIENT_ControlDevice(LoginModule.m_hLoginHandle,
                CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, insert.getPointer(), 5000);
        insert.read();
        accessCardInfo.read();

        if (!bRet) {
            logger.error("添加卡信息失败." + ToolKits.getErrorCodePrint());
            LoginModule.logout();
            return 0;
        } else {
            logger.info("添加卡信息成功,卡信息记录集编号 : " + insert.stuCtrlRecordSetResult.nRecNo);
        }
        addFaceInfo(map.get(Constant.userId), map);
        return insert.stuCtrlRecordSetResult.nRecNo;
    }

    /**
     * 添加人脸
     *
     * @param userId 用户ID
     * @param map    图片缓存
     * @return
     */
    private boolean addFaceInfo(String userId, Map<String, String> map) {
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_ADD;  // 添加

        //人脸
        String filePath = map.get(Constant.photoPath);
//        try {
//            Map<Integer, String> fileMap = FileUtils.readfile(filePath, null);
//            Thread.sleep(1000);
//            for (int i = 0; i < fileMap.size(); i++) {
//                FileUtils.compressImage(fileMap.get(i), "E:\\sts-space\\photoCache\\staff\\" + map.get(Constant.username) + " .jpg", 390, 520);
//                filePath = "E:\\sts-space\\photoCache\\staff\\" + map.get(Constant.username) + " .jpg";
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Memory memory = ToolKits.readPictureFile(filePath);

        /**
         *  入参
         */
        NET_IN_ADD_FACE_INFO stIn = new NET_IN_ADD_FACE_INFO();

        // 用户ID
        System.arraycopy(userId.getBytes(), 0, stIn.szUserID, 0, userId.getBytes().length);

        // 人脸照片个数
        stIn.stuFaceInfo.nFacePhoto = 1;

        // 每张图片的大小
        stIn.stuFaceInfo.nFacePhotoLen[0] = (int) memory.size();

        // 人脸照片数据,大小不超过100K, 图片格式为jpg
        stIn.stuFaceInfo.pszFacePhotoArr[0].pszFacePhoto = memory;

        /**
         *  出参
         */
        NET_OUT_ADD_FACE_INFO stOut = new NET_OUT_ADD_FACE_INFO();

        stIn.write();
        stOut.write();
        boolean bRet = LoginModule.netsdk.CLIENT_FaceInfoOpreate(LoginModule.m_hLoginHandle, emType, stIn.getPointer(), stOut.getPointer(), 5000);
        stIn.read();
        stOut.read();
        if (bRet) {
            logger.info("添加人脸成功!");
        } else {
            logger.error("添加人脸失败!" + ToolKits.getErrorCodePrint());
            LoginModule.logout();
            return false;
        }
        //退出登录
        LoginModule.logout();
        return true;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            getStaff();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void login() {
        try {
            String url = "http://192.168.0.210:80/auth/login";

            NameValuePair[] data = {
                    new NameValuePair("username", "test@megvii.com"),
                    new NameValuePair("password", "kl123456"),
                    new NameValuePair("auth_token", "true")
            };
            String response = "";//要返回的response信息
            HttpClient httpClient = new HttpClient();
            PostMethod postMethod = new PostMethod(url);
            postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            postMethod.addRequestHeader("user-agent", "Koala Admin");
            // 将表单的值放入postMethod中
            postMethod.setRequestBody(data);
            // 执行postMethod
            int statusCode = 0;
            try {
                statusCode = httpClient.executeMethod(postMethod);
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //状态
            int code = 0;
            // HttpClient对于要求接受后继服务的请求，象POST和PUT等不能自动处理转发
            // 301或者302
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                    || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                // 从头中取出转向的地址
                Header locationHeader = postMethod.getResponseHeader("location");
                String location = null;
                if (locationHeader != null) {
                    location = locationHeader.getValue();
                    System.out.println("The page was redirected to:" + location);
                } else {
                    System.err.println("Location field value is null.");
                }
            } else {
                System.out.println("登录状态:" + postMethod.getStatusLine());
                code = postMethod.getStatusLine().getStatusCode();

                try {
                    response = postMethod.getResponseBodyAsString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                postMethod.releaseConnection();
            }
            JSONObject parse = JSON.parseObject(response);
            JSONObject Data = JSON.parseObject(parse.getString("data"));
            token = (String) Data.get("auth_token");

            if (code == 200) {
                logger.info("登录成功~");
            } else {
                logger.error("登录失败~");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("登录异常~");
        }
    }

    /**
     * 将字符串转为时间戳
     */
    public static long getTimeStamp(String time) {
        SimpleDateFormat sf = null;

        sf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();
        try {
            date = sf.parse(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

}
