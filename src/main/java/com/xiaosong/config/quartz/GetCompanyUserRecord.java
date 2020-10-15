package com.xiaosong.config.quartz;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SmUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.date.Constant;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;
import com.xiaosong.common.Koala.StaffController;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.personnel.PersonnelService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.wincc.failreceive.FailReceService;
import com.xiaosong.common.wincc.companyuser.StaffService;
import com.xiaosong.config.SendAccessRecord;
import com.xiaosong.constant.Constants;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbFailreceive;
import com.xiaosong.util.*;
import com.xiaosong.util.QRCodeModel.GetCompanyUserScheduleModel;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时拉取员工数据 并下发
 */
@DisallowConcurrentExecution
public class GetCompanyUserRecord implements Job {
    private StaffService srvStaff = StaffService.me;    //员工业务层
    private DeviceService srvDevice = DeviceService.me; //设备业务层
    private FailReceService srvFail = FailReceService.me; //下发失败业务层
    private ServerService srvServer = ServerService.me; //服务器业务层
    private SendAccessRecord sendAccessRecord = new SendAccessRecord(); //下发的通行记录
    private PersonnelService personService = PersonnelService.me; //人员业务层
    private static Logger logger = Logger.getLogger(GetCompanyUserRecord.class); //日志
    private OkHttpUtil okHttpUtil = new OkHttpUtil();
    private Map<String, String> sendMap = new HashMap<>();
    private Cache cache = Redis.use("xiaosong"); //缓存
    public static String token = null;
    private Prop use = PropKit.use("config_product.properties");

    private static String key = "iB4drRzSrC";
    /**
     * 获取人员信息
     *
     * @throws Exception
     */
    public void getOrgInformation() throws Exception {
        TbBuildingServer tbServerinfo = srvServer.findSer();
        if (tbServerinfo.getOrgCode().isEmpty()) {
            logger.error("大楼编号不存在.");
            return;
        }
        logger.info("开始更新大楼员工数据..");

        Map<String, String> map = new HashMap<>();
        map.put("org_code", tbServerinfo.getOrgCode());
        String keysign = tbServerinfo.getOrgCode() + tbServerinfo.getPospCode() + tbServerinfo.getKey();
        String sign = MD5Util.MD5(keysign);
        map.put("sign", sign);
        StringBuilder stringBuilder = new StringBuilder();
        //stringBuilder.append(Constants.baseURl);

        stringBuilder.append("http://" + tbServerinfo.getServerIp() + ":" + tbServerinfo.getServerPort() + "/visitor/");
        stringBuilder.append(Constants.pullOrgCompanyUrl);
//        stringBuilder.append(Constants.ceshi);
        String url = stringBuilder.toString();
        logger.info("获取员工数据地址：" + url);

        String responseContent = okHttpUtil.post(url, map);

        if (responseContent != null) {
            GetCompanyUserScheduleModel getStaffScheduleResponseParentModel = JSON.parseObject(responseContent,
                    GetCompanyUserScheduleModel.class);
            if(getStaffScheduleResponseParentModel==null){
                logger.error("网络错误...");
                return;
            }
            // System.out.println(getStaffScheduleResponseParentModel.toString());
            if (null != getStaffScheduleResponseParentModel.getData()) {
                List<TbCompanyuser> companyUserList = getStaffScheduleResponseParentModel.getData();
                logger.info("需要拉取数据" + companyUserList.size() + "条");
                if (companyUserList == null || companyUserList.size() == 0) {
                    logger.warn("无新员工数据!");
                } else {
                    for (TbCompanyuser companyUser : companyUserList) {
                        //	System.out.println(companyUser.toString());
                        companyUser.setIsDel("1");
                        companyUser.setIsSued("1");
                        TbCompanyuser userfind = srvStaff.findByNameAndIdNO(companyUser.getUserName(),
                                companyUser.getIdNO(), "normal");
                        if (userfind == null) {
                            notExitUser(companyUser);
                        } else {
                            doExitUser(userfind, companyUser);
                        }
                    }
                }
            }
        } else {
            logger.error(url);
            //logger.sendErrorLog(towerInforService.findOrgId(), "请求网址"+url+"获取的数据为空", "","网络错误", Constants.errorLogUrl,keysign);
        }

        // 查找今天之前下发错误的
        List<TbCompanyuser> companyUsers = srvStaff.findBeforeToDay(getDate());
        if (companyUsers.size() == 0 || companyUsers == null) {
            logger.info("未出现下发错误的名单");
        } else {
            for (TbCompanyuser user : companyUsers) {
                if (!user.getCurrentStatus().equals("normal")) {
                    continue;
                }
                String issued = "0";
                List<TbFailreceive> faceFails = srvFail.findByName(user.getUserName(), "save");
                if (faceFails.size() == 0 || faceFails == null) {
                    logger.error("失败记录表无" + user.getUserName() + "数据");
                    continue;
                }
                for (TbFailreceive faceReceive : faceFails) {
                    String photo = isPhoto(user);
                    if (photo == null) {
                        logger.error("缺失照片");
                        //logger.sendErrorLog(towerInforService.findOrgId(), user.getUserName()+"缺失照片", "","数据错误", Constants.errorLogUrl,keysign);
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
                        sendMap.put(Constants.currentStatus, user.getCurrentStatus());
                        sendMap.put(Constants.userName, user.getUserName());
                        sendMap.put(Constants.idNo, user.getIdNO());
                        isSuccess = personService.sendFaceHJ(faceReceive.getFaceIp(), sendMap, photo);
                    } else if (device.getDeviceType().equals("DS-K5671")) {

                        isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), user, null);

                    } else if (device.getDeviceType().equals("DS-K5671-H")) {
                        isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), user, null);
                        isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, user.getUserId(), user.getUserName(), "S" + user.getUserId(),null,null);

                    }  else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                        File picAppendData = IPCxmlFile(user);
                        String filePath = Constants.StaffPath + "/" + user.getPhoto() + ".jpg";
                        File picture = new File(filePath);
//                        isSuccess = sendAccessRecord.sendToIPC(faceReceive.getFaceIp(), picture, picAppendData, user, null, device.getAdmin(), device.getPassword());
                    } else if (device.getDeviceType().equals("DH-ASI728")) {

                        sendMap.put(Constant.deviceIp, faceReceive.getFaceIp());   //设备ip*
                        sendMap.put(Constant.photoPath, photo);                 //图片*
                        sendMap.put(Constant.userId, String.valueOf(user.getUserId()));       //用户id
                        sendMap.put(Constant.username, user.getUserName());   //用户姓名*
                        sendMap.put(Constants.idNo, user.getIdNO());           //身份证号码
                        int cardInfo=personService.insertInfo(sendMap);
                        if(cardInfo==0){
                            isSuccess=false;
                        }else{
                            user.setCardInfo(cardInfo);
                            user.update();
                        }


                    }else if(device.getDeviceType().equals("NL-RZ810")){
                        String number = DESUtil.decode(key,user.getIdNO());
                        String name = user.getUserName();
//                            byte[] data = FilesUtils.compressUnderSize(deluser.getPhoto().getBytes(), 40960L);
//                            String encode = Base64.encode(data);
                        String type = "1";
                        String startTime = "1594369310867";
                        String endTime = "1994362053814";
                        String bid = user.getBid();
                        JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "",type,startTime,endTime,bid,"",device.getDeviceName());
                        if ("0".equals(jsonObject.getString("code"))) {
                            isSuccess=true;
                            String data1 = jsonObject.getString("data");
                            if (StringUtils.isNotBlank(data1)) {
                                data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                JSONObject value = JSON.parseObject(data1);
                                System.out.println(data1);
                                logger.info("data信息为{}" + value.toJSONString());
                                String rid = value.getString("rid");
                                logger.info("服务端响应解密后数据：" + jsonObject);
                                user.setRid(rid);
                                user.update();
                            }
                        } else {
                            isSuccess=false;
                            logger.info("失败原因：{}" + jsonObject.getString("msg"));
                        }
                    }else if (device.getDeviceType().equals("KS-250")) {
                        if (token == null) {
                            login();
                        }
                        String urlPath = use.get("url");
                        String path = "http://" + urlPath + ":80/subject/file";

                        String str = StaffController.doPost(path, user.getUserName(), "0", photo, null, null,token);


                        JSONObject parse = JSONObject.parseObject(str);

                        String data = parse.getString("data");
                        JSONObject par = JSONObject.parseObject(data);
                        String id = par.getString("id");

                        Integer code = (Integer) parse.get("code");
                        if(code==0){
                            isSuccess = true;
                            user.setUserId(Integer.valueOf(id));
                        }else{
                            isSuccess = false;
                        }
                    }
                    if (!isSuccess||!isSuccess2) {
                        issued = "1";
                        logger.error("失败名单下发" + user.getUserName() + "再次失败");
                        //logger.sendErrorLog(towerInforService.findOrgId(), "失败名单下发" + user.getUserName() + "再次失败", "人脸设备IP"+faceReceive.getFaceIp(),"下发错误", Constants.errorLogUrl,keysign);
                        int count = faceReceive.getDownNum() + 1;
                        faceReceive.setDownNum(count);
                        faceReceive.update();
                    } else {
                        Db.update("UPDATE tb_failreceive SET receiveFlag = '0' WHERE userName = ? and receiveTime= ?", faceReceive.getUserName(),faceReceive.getReceiveTime());
                    }
                }
                user.setIsSued(issued);
                user.update();
            }
        }

        //处理未删除的员工数据
        List<TbCompanyuser> faliList = srvStaff.findFailDel();
        if (faliList.size() == 0 || faliList == null) {
            logger.info("未出现未删除的名单");
        } else {
            for (TbCompanyuser deluser : faliList) {
                String photo = isPhoto(deluser);
                if (null == photo) {
                    return;
                }
                String companyFloor = deluser.getCompanyFloor();
                List<String> allFaceDecive = srvDevice.getAllFaceDeviceIP(companyFloor);
                if (allFaceDecive.size() > 0) {
                    String isdel = "0";
                    for (int i = 0; i < allFaceDecive.size(); i++) {

                        TbDevice device = srvDevice.findByDeviceIp(allFaceDecive.get(i));
                        if (null == device) {
                            logger.error("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                            continue;
                        }
                        boolean isSuccess = true;
                        boolean isSuccess2 = true;
                        if (device.getDeviceType().equals("TPS980")) {
                            sendMap.put(Constants.currentStatus, deluser.getCurrentStatus());
                            sendMap.put(Constants.userName, deluser.getUserName());
                            sendMap.put(Constants.idNo, deluser.getIdNO());
                            isSuccess = personService.sendFaceHJ(allFaceDecive.get(i), sendMap, photo);
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            if (deluser.getIsDel().equals("0")) {
                                logger.info(deluser.getUserName() + "已删除名单");
                                continue;
                            } else {

                                isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), deluser, null);

                            }
                        } else if (device.getDeviceType().equals("DS-K5671-H")) {
                            isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), deluser, null);
                            isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, deluser.getUserId(), deluser.getUserName(), "S" + deluser.getUserId(),null,null);

                        } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
//                            isSuccess = sendAccessRecord.delIPCpicture("normal", deluser.getIdFrontImgUrl());

                        } else if (device.getDeviceType().equals("DH-ASI728")) {

                            sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                            isSuccess = personService.deleteDH(sendMap, deluser.getCardInfo());

                        }else if(device.getDeviceType().equals("NL-RZ810")){
                            String number = DESUtil.decode(key,deluser.getIdNO());
                            String name = deluser.getUserName();
//                            byte[] data = FilesUtils.compressUnderSize(deluser.getPhoto().getBytes(), 40960L);
//                            String encode = Base64.encode(data);
                            String type = "1";
                            String startTime = "1594369310867";
                            String endTime = "1994362053814";
                            String bid = deluser.getBid();
                            JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "",type,startTime,endTime,bid,"",device.getDeviceName());
                            if ("0".equals(jsonObject.getString("code"))) {
                                isSuccess=true;

                                String data1 = jsonObject.getString("data");
                                if (StringUtils.isNotBlank(data1)) {
                                    data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                    JSONObject value = JSON.parseObject(data1);
                                    System.out.println(data1);
                                    logger.info("data信息为{}" + value.toJSONString());
                                    String rid = value.getString("rid");
                                    logger.info("服务端响应解密后数据：" + jsonObject);
                                    deluser.setRid(rid);
                                    deluser.update();
                                }
                            } else {
                                isSuccess=false;
                                logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            }
                        }else if (device.getDeviceType().equals("KS-250")) {
                            if (token == null) {
                                login();
                            }
                            String urlPath = use.get("url");

                            String path = "http://" + urlPath + ":80/subject/" + deluser.getUserId();
                            String str = StaffController.doDelete(path, token);
                            JSONObject jsonObject = JSONObject.parseObject(str);
                            Integer code = (Integer) jsonObject.get("code");
                            if(code==0){
                                isSuccess=true;
                            }else{
                                isSuccess=false;
                            }
                        }

                        if (isSuccess&&isSuccess2) {
                            logger.info("设备IP" + allFaceDecive.get(i) + "删除" + deluser.getUserName() + "成功");
                        } else {
                            isdel = "1";
                            TbFailreceive faceReceive = srvFail.findOne(allFaceDecive.get(i), deluser.getUserName(), deluser.getIdNO(), "staff");
                            if (null == faceReceive) {

                                TbFailreceive newFaceFail = new TbFailreceive();
                                newFaceFail.setFaceIp(allFaceDecive.get(i));
                                newFaceFail.setIdCard(deluser.getIdNO());
                                newFaceFail.setUserName(deluser.getUserName());
                                newFaceFail.setReceiveFlag("1");
                                newFaceFail.setUserType("staff");
                                newFaceFail.setDownNum(0);
                                newFaceFail.setOpera("deleted");
                                newFaceFail.setReceiveTime(getDateTime());
                                newFaceFail.save();
                            } else {
                                int count = faceReceive.getDownNum();
                                count = count + 1;
                                faceReceive.setDownNum(count);
                                faceReceive.update();
                            }
                        }
                    }
                    deluser.setIsDel(isdel);
                    deluser.update();
                }
            }
        }
    }

    /**
     * 不是在职 的员工 不接收
     *
     * @param companyUser 员工
     * @throws Exception
     */
    private void notExitUser(TbCompanyuser companyUser) throws Exception {

        // 无状态员工不接收
        if (!"normal".equals(companyUser.getCurrentStatus())) {
            logger.info("员工" + companyUser.getUserName() + "的状态是" + companyUser.getCurrentStatus() + ",上位机不接收");
            return;
        }
        TbCompanyuser userfind = srvStaff.findByNameAndIdNO(companyUser.getUserName(),
                companyUser.getIdNO(), "deleted");

        if (null != userfind) {
            srvStaff.deleteOne(userfind);
        }
        companyUser.setReceiveDate(getDate());
        companyUser.setReceiveTime(getTime());
        String fileName = null;

        if (companyUser.getPhoto() != null) {
//            CacheKit.put("xiaosong", "photo_" + companyUser.getCompanyId() + "_" + companyUser.getIdNO(), companyUser.getPhoto());
            byte[] photoKey = Base64_2.decode(companyUser.getPhoto());
            fileName = companyUser.getUserName() + companyUser.getUserId() + ".jpg";
            File fromBytes = FilesUtils.getFileFromBytes(photoKey, Constants.StaffPath, fileName);
            logger.info("初始化员工存放照片地址" + fromBytes.getAbsolutePath());

            File file = new File(Constants.StaffPath+fileName);

            String name = file.getName();
            String absolutePath = file.getAbsolutePath();
            String path = file.getParentFile().getPath();

            String b= Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024*90L));
            byte[] photoKey1 = Base64_2.decode(b);
            FilesUtils.getFileFromBytes(photoKey1, Constants.StaffPath, name);
            companyUser.setPhoto(fromBytes.getAbsolutePath());
        } else {
            logger.error((companyUser.getUserName() + "该用户无照片"));
            //logger.sendErrorLog(towerInforService.findOrgId(), companyUser.getUserName() + "该用户无照片", "","数据错误", Constants.errorLogUrl,keysign);
            return;
        }
        companyUser.save();

        String companyfloor = companyUser.getCompanyFloor();
        if (null != companyUser.getCompanyFloor()) {
            companyfloor = companyUser.getCompanyFloor();
        }

        List<String> allFaceDecive = srvDevice.getAllFaceDeviceIP(companyfloor);
        System.out.println(allFaceDecive.size());
        if (allFaceDecive.size() > 0) {
            String issued = "0";
            for (int i = 0; i < allFaceDecive.size(); i++) {
                logger.info("需下发的人像识别仪器IP为：" + allFaceDecive.get(i));

                TbDevice device = srvDevice.findByDeviceIp(allFaceDecive.get(i));
                if (null == device) {
                    logger.error("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                    continue;
                }
                String photo = isPhoto(companyUser);
                boolean isSuccess = true;
                boolean isSuccess2 = true;
                if (device.getDeviceType().equals("TPS980")) {
                    sendMap.put(Constants.currentStatus, companyUser.getCurrentStatus());
                    sendMap.put(Constants.userName, companyUser.getUserName());
                    sendMap.put(Constants.idNo, companyUser.getIdNO());
                    isSuccess = personService.sendFaceHJ((String) allFaceDecive.get(i), sendMap,
                            companyUser.getPhoto());
                } else if (device.getDeviceType().equals("DS-K5671")) {

                    isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), companyUser, null);

                } else if (device.getDeviceType().equals("DS-K5671-H")) {
                    isSuccess = sendAccessRecord.setCardAndFace(allFaceDecive.get(i), companyUser, null);
                    isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, companyUser.getUserId(), companyUser.getUserName(), "S" + companyUser.getUserId(),null,null);

                } else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                    File picAppendData = IPCxmlFile(companyUser);
                    String filePath = Constants.StaffPath + "/" + companyUser.getPhoto() + ".jpg";
                    File picture = new File(filePath);
//                    isSuccess = sendAccessRecord.sendToIPC((String) allFaceDecive.get(i), picture, picAppendData, companyUser, null, device.getAdmin(), device.getPassword());
                } else if (device.getDeviceType().equals("DH-ASI728")) {

                    sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                    sendMap.put(Constant.photoPath, photo);      //图片*
                    sendMap.put(Constant.userId, String.valueOf(companyUser.getUserId()));       //用户id
                    sendMap.put(Constants.userName, companyUser.getUserName());   //用户姓名*
                    sendMap.put(Constants.idNo, companyUser.getIdNO());           //身份证号码
                    int cardInfo=personService.insertInfo(sendMap);
                    if(cardInfo==0){
                        isSuccess=false;
                    }else{
                        companyUser.setCardInfo(cardInfo);
                        companyUser.update();
                    }

                }else if(device.getDeviceType().equals("NL-RZ810")){
                    String number = DESUtil.decode(key,companyUser.getIdNO());
                    String name = companyUser.getUserName();
                    byte[] data = FilesUtils.compressUnderSize(companyUser.getPhoto().getBytes(), 40960L);
                    String encode = Base64.encode(data);
                    String type = "1";
                    String startTime = "1594369310867";
                    String endTime = "1994362053814";
                    String bid = companyUser.getBid();
                    JSONObject jsonObject = NewWorldAuth.sendPost(number, name, "",type,startTime,endTime,bid,"",device.getDeviceName());
                    if ("0".equals(jsonObject.getString("code"))) {
                        isSuccess=true;
                        String data1 = jsonObject.getString("data");
                        if (StringUtils.isNotBlank(data1)) {
                            data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                            JSONObject value = JSON.parseObject(data1);
                            System.out.println(data1);
                            logger.info("data信息为{}" + value.toJSONString());
                            String rid = value.getString("rid");
                            logger.info("服务端响应解密后数据：" + jsonObject);
                            companyUser.setRid(rid);
                            companyUser.update();
                        }
                    } else {
                        logger.info("失败原因：{}" + jsonObject.getString("msg"));
                        isSuccess=false;
                    }
                }else if (device.getDeviceType().equals("KS-250")) {
                    if (token == null) {
                        login();
                    }
                    String urlPath = use.get("url");
                    String path = "http://" + urlPath + ":80/subject/file";

                    String str = StaffController.doPost(path, companyUser.getUserName(), "0", photo, null, null,token);


                    JSONObject parse = JSONObject.parseObject(str);

                    String data = parse.getString("data");
                    JSONObject par = JSONObject.parseObject(data);
                    String id = par.getString("id");

                    Integer code = (Integer) parse.get("code");
                    if(code==0){
                        isSuccess = true;
                        companyUser.setUserId(Integer.valueOf(id));
                    }else{
                        isSuccess = false;
                    }
                }
                // 针对下发失败的需要登记，待下次冲洗下发，已经下发成功的不在下发
                if (!isSuccess||!isSuccess2) {
                    issued = "1";
                    TbFailreceive faceReceive = srvFail.findOne(allFaceDecive.get(i), companyUser.getUserName(), companyUser.getIdNO(), "staff");
                    if (null == faceReceive) {
                        TbFailreceive newFaceFail = new TbFailreceive();
                        newFaceFail.setFaceIp(allFaceDecive.get(i));
                        newFaceFail.setIdCard(companyUser.getIdNO());
                        newFaceFail.setUserName(companyUser.getUserName());
                        newFaceFail.setReceiveFlag("1");
                        newFaceFail.setUserType("staff");
                        newFaceFail.setDownNum(0);
                        newFaceFail.setOpera("save");
                        newFaceFail.setReceiveTime(getDateTime());
                        newFaceFail.save();
                    } else {
                        int count = faceReceive.getDownNum();
                        count = count + 1;
                        System.out.println(count);
                        faceReceive.setDownNum(count);
                        faceReceive.update();
                    }
                    logger.info("失败表记录" + companyUser.getUserName() + "数据");
                }
            }
            companyUser.setIsSued(issued);
            companyUser.update();
        }
    }

    private void doExitUser(TbCompanyuser companyUser, TbCompanyuser newUser) throws Exception {

        if (newUser.getCurrentStatus().equals("normal")) {
            if (companyUser.getIsSued().equals("0")) {
                return;
            } else {
                List<TbFailreceive> faceReceiveList = srvFail.findByName(companyUser.getUserName(), "staff");
                if (faceReceiveList.size() <= 0) {

                    return;
                } else {

                    String issued = "0";
                    for (TbFailreceive faceReceive : faceReceiveList) {
                        TbCompanyuser user = srvStaff.findByNameAndIdNO(faceReceive.getUserName(),
                                faceReceive.getIdCard(), "normal");

                        String photo = isPhoto(user);
                        if (photo == null) {
                            logger.error("缺失照片");
                            //logger.sendErrorLog(towerInforService.findOrgId(),user.getUserName()+ "缺失照片", "","数据错误", Constants.errorLogUrl,keysign);
                            continue;
                        }
                        TbDevice device = srvDevice.findByDeviceIp(faceReceive.getFaceIp());
                        if (null == device) {
                            logger.error("设备表缺少IP为" + faceReceive.getFaceIp() + "的设备");
                            continue;
                        }
                        boolean isSuccess = true;
                        boolean isSuccess2 = true;
                        if (device.getDeviceType().equals("TPS980")) {
                            sendMap.put(Constants.currentStatus, companyUser.getCurrentStatus());
                            sendMap.put(Constants.userName, companyUser.getUserName());
                            sendMap.put(Constants.idNo, companyUser.getIdNO());
                            isSuccess = personService.sendFaceHJ(faceReceive.getFaceIp(), sendMap,
                                    photo);
                        } else if (device.getDeviceType().equals("DS-K5671")) {

                            isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), user, null);

                        } else if (device.getDeviceType().equals("DS-K5671-H")) {
                            isSuccess = sendAccessRecord.setCardAndFace(faceReceive.getFaceIp(), companyUser, null);
                            isSuccess2 = sendAccessRecord.setCardInfo(Constants.deviceGate, companyUser.getUserId(), companyUser.getUserName(), "S" + companyUser.getUserId(),null,null);

                        }  else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                            File picAppendData = IPCxmlFile(user);
                            String filePath = Constants.StaffPath + "/" + user.getUserName() + user.getCompanyId()
                                    + ".jpg";
                            File picture = new File(filePath);
//                            isSuccess = sendAccessRecord.sendToIPC(faceReceive.getFaceIp(), picture, picAppendData, user, null, device.getAdmin(), device.getPassword());
                        } else if (device.getDeviceType().equals("DH-ASI728")) {

                            sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                            sendMap.put(Constant.photoPath, photo);      //图片*
                            sendMap.put(Constant.userId, String.valueOf(companyUser.getUserId()));       //用户id
                            sendMap.put(Constant.username, companyUser.getUserName());   //用户姓名*
//                            sendMap.put(Constants.idNo, companyUser.getIdNO());           //身份证号码
                            int cardInfo=personService.insertInfo(sendMap);
                            if(cardInfo==0){
                                isSuccess=false;
                            }else{
                                companyUser.setCardInfo(cardInfo);
                                companyUser.update();
                            }

                        }else if(device.getDeviceType().equals("NL-RZ810")){
                            String number = DESUtil.decode(key,companyUser.getIdNO());
                            String name = companyUser.getUserName();
                            byte[] data = FilesUtils.compressUnderSize(companyUser.getPhoto().getBytes(), 40960L);
                            String encode = Base64.encode(data);
                            String type = "1";
                            String startTime = "1594369310867";
                            String endTime = "1994362053814";
                            String bid = companyUser.getBid();
                            JSONObject jsonObject = NewWorldAuth.sendPost(number, name, encode,type,startTime,endTime,bid,"",device.getDeviceName());
                            if ("0".equals(jsonObject.getString("code"))) {
                                isSuccess=true;

                                String data1 = jsonObject.getString("data");
                                if (StringUtils.isNotBlank(data1)) {
                                    data1 = SmUtil.sm4(NewWorldAuth.SERVER_KEY.getBytes()).decryptStrFromBase64(data1);
                                    JSONObject value = JSON.parseObject(data1);
                                    System.out.println(data1);
                                    logger.info("data信息为{}" + value.toJSONString());
                                    String rid = value.getString("rid");
                                    logger.info("服务端响应解密后数据：" + jsonObject);
                                    companyUser.setRid(rid);
                                    companyUser.update();
                                }
                            } else {
                                isSuccess=false;

                                logger.info("失败原因：{}" + jsonObject.getString("msg"));
                            }
                        }else if (device.getDeviceType().equals("KS-250")) {
                            if (token == null) {
                                login();
                            }
                            String urlPath = use.get("url");
                            String path = "http://" + urlPath + ":80/subject/file";

                            String str = StaffController.doPost(path, user.getUserName(), "0", photo, null, null,token);


                            JSONObject parse = JSONObject.parseObject(str);

                            String data = parse.getString("data");
                            JSONObject par = JSONObject.parseObject(data);
                            String id = par.getString("id");

                            Integer code = (Integer) parse.get("code");
                            if(code==0){
                                isSuccess = true;
                                user.setUserId(Integer.valueOf(id));
                            }else{
                                isSuccess = false;
                            }
                        }
                        if (!isSuccess || !isSuccess2) {
                            issued = "1";
                            logger.error("失败名单下发" + user.getUserName() + "再次失败");
                            //logger.sendErrorLog(towerInforService.findOrgId(), "失败名单下发" + user.getUserName() + "再次失败", "人脸设备IP"+faceReceive.getFaceIp(),"设备接收错误", Constants.errorLogUrl,keysign);
                            int count = faceReceive.getDownNum() + 1;
                            faceReceive.setDownNum(count);
                            faceReceive.update();
                        } else {
                            Db.update("UPDATE tb_failreceive SET receiveFlag = '0' WHERE userName = ? and receiveTime= ?", faceReceive.getUserName(),faceReceive.getReceiveTime());
                        }
                    }
                    companyUser.setIsSued(issued);
                    companyUser.update();
                }
                return;
            }
        } else if (newUser.getCurrentStatus().equals("deleted")) {
            if (companyUser.getIsSued().equals("1")) {
                return;
            }
            String photo = newUser.getPhoto();
            if (null == photo) {
                photo = isPhoto(companyUser);
            }
            String companyfloor = companyUser.getCompanyFloor();
            List<String> allFaceDecive = srvDevice.getAllFaceDeviceIP(companyfloor);
            if (allFaceDecive.size() > 0) {
                String isdel = "0";
                for (int i = 0; i < allFaceDecive.size(); i++) {
                    TbDevice device = srvDevice.findByDeviceIp(allFaceDecive.get(i));
                    if (null == device) {
                        logger.error("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                        continue;
                    }
                    boolean isSuccess = true;
                    if (device.getDeviceType().equals("TPS980")) {
                        srvStaff.sendDelWhiteList(allFaceDecive.get(i), companyUser.getUserName(), companyUser.getIdNO());
                    } else if (device.getDeviceType().equals("DS-K5671")) {

                        String cardStr = "S" + companyUser.getUserId();
                        isSuccess = sendAccessRecord.delFace(allFaceDecive.get(i), cardStr);
                        if (isSuccess) {
                            sendAccessRecord.delCard(allFaceDecive.get(i), companyUser, null);

                        }
                    }  else if (device.getDeviceType().equals("DS-2CD8627FWD")) {
                        //linux 下 加载 海康sdk
//                        InitHCNetSDK.run(device.getDeviceType());
                        // winds 下海康设备就初始化海康SDK
//                      devicesInit.initHC();
//                      isSuccess = sendAccessRecord.delIPCpicture("staff", companyUser.getIdFrontImgUrl());
                    } else if (device.getDeviceType().equals("DH-ASI728")) {
                        sendMap.put(Constant.deviceIp, device.getDeviceIp());   //设备ip*
                        isSuccess=personService.deleteDH(sendMap, companyUser.getCardInfo());

                    }else if(device.getDeviceType().equals("NL-RZ810")){
                        String number = DESUtil.decode(key,companyUser.getIdNO());
                        String name = companyUser.getUserName();
                        byte[] data = FilesUtils.compressUnderSize(companyUser.getPhoto().getBytes(), 40960L);
                        String encode = Base64.encode(data);
                        String type = "3";
                        String startTime = "1594369310867";
                        String endTime = "1994362053814";
                        String bid = companyUser.getBid();
                        String rid = companyUser.getRid();
                        JSONObject jsonObject = NewWorldAuth.sendPost(number, name, encode,type,startTime,endTime,bid,rid,device.getDeviceName());
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
                    }else if (device.getDeviceType().equals("KS-250")) {
                        if (token == null) {
                            login();
                        }
                        String url = use.get("url");

                        String path = "http://" + url + ":80/subject/" + companyUser.getUserId();
                        String str = StaffController.doDelete(path, token);
                        JSONObject jsonObject = JSONObject.parseObject(str);
                        Integer code = (Integer) jsonObject.get("code");
                        if(code==0){
                            isSuccess=true;
                        }else{
                            isSuccess=false;
                        }
                    }
                    if (isSuccess) {
                        logger.info("设备IP" + allFaceDecive.get(i) + "删除" + companyUser.getUserName() + "成功");
                    } else {
                        isdel = "1";
                        TbFailreceive faceReceive = srvFail.findOne(allFaceDecive.get(i), companyUser.getUserName(), companyUser.getIdNO(), "staff");
                        if (null == faceReceive) {
                            TbFailreceive newFaceFail = new TbFailreceive();
                            newFaceFail.setFaceIp(allFaceDecive.get(i));
                            newFaceFail.setIdCard(companyUser.getIdNO());
                            newFaceFail.setUserName(companyUser.getUserName());
                            newFaceFail.setReceiveFlag("1");
                            newFaceFail.setUserType("staff");
                            newFaceFail.setDownNum(0);
                            newFaceFail.setOpera("deleted");
                            newFaceFail.setReceiveTime(getDateTime());
                            newFaceFail.save();
                        } else {
                            int count = faceReceive.getDownNum();
                            count = count + 1;
                            faceReceive.setDownNum(count);
                            faceReceive.update();
                        }
                    }
                }
                companyUser.setIsDel(isdel);
                companyUser.setCurrentStatus("deleted");
                companyUser.update();
            }
        } else

        {

        }

    }

    /**
     * 照片
     *
     * @param companyUser 用户信息
     * @return
     * @throws Exception
     */
    private String isPhoto(TbCompanyuser companyUser) throws Exception {

//       String photo= CacheKit.get("xiaosong","photo_" + companyUser.getCompanyId() + "_" + companyUser.getIdNO());
//        if (photo == null) {
        String filePath = companyUser.getPhoto();

        //String filePath = "E:\\sts-space\\photoCache\\staff\\" + companyUser.getUserName()+ companyUser.getCompanyId() + ".jpg";
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error(companyUser.getUserName() + "无照片");
            //logger.sendErrorLog(towerInforService.findOrgId(), companyUser.getUserName() + "无照片", "","数据错误", Constants.errorLogUrl,keysign);
            return null;
//            } else {
//                photo = Base64_2.encode(FilesUtils.getBytesFromFile(file));
//                CacheKit.put("xiaosong","photo_" + companyUser.getCompanyId() + "_" + companyUser.getIdNO(), photo);
//            }
        }
        return filePath;
    }

    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 下发IPC人像时所需照片附加信息文件
     *
     * @param user
     * @return
     */
    public File IPCxmlFile(TbCompanyuser user) {
        // TODO Auto-generated method stub
        String filePath = Constants.StaffPath + "/" + user.getPhoto() + ".xml";
        File filepath = new File(Constants.StaffPath);
        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        File file = new File(filePath);

        StringBuilder builder = new StringBuilder();
        builder.append("<FaceAppendData><name>S");
        builder.append(user.getUserName());
        builder.append("</name><certificateType>ID</certificateType><certificateNumber>");
        builder.append(user.getUserId());
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
     *  旷视设备登录
     */
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

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            getOrgInformation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
