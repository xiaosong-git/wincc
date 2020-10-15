package com.xiaosong.common.wincc.companyuser;

import com.alibaba.fastjson.JSON;
import com.jfinal.core.Controller;
import com.xiaosong.common.wincc.buildingserverservice.BuildingServerService;
import com.xiaosong.common.wincc.device.TbDeviceService;
import com.xiaosong.common.wincc.devicerelated.TbDevicerelatedService;
import com.xiaosong.common.wincc.failreceive.FailReceiveService;
import com.xiaosong.constant.Constant;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbFailreceive;
import com.xiaosong.responsemodel.companyusers.CompanyUserList;
import com.xiaosong.responsemodel.companyusers.InitParentModel;
import com.xiaosong.sdkConfig.HCNetSDKService;
import com.xiaosong.util.Base64_2;
import com.xiaosong.util.FilesUtils;
import com.xiaosong.util.OkHttpUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitCompanyUsersController extends Controller {

    Logger logger = Logger.getLogger(InitCompanyUsersController.class);

    private TbCompanyUserService companyUserService = TbCompanyUserService.me;

    private BuildingServerService serverService = BuildingServerService.me;

    private TbDevicerelatedService devRelatedService = TbDevicerelatedService.me;

    private TbDeviceService devicesService = TbDeviceService.me;

    private HCNetSDKService hcNetSDKService = HCNetSDKService.me;

    private FailReceiveService failReceiveService = FailReceiveService.me;
    OkHttpUtil okHttpUtil = new OkHttpUtil();

    public void index() throws Exception {

        int pageNum = 1;
        int pageSize = 10;
        int totalPage =0;
        int total = 0;

        TbBuildingServer building = serverService.findInfo();
        Map<String, String> map = new HashMap<>();
        map.put("org_code", building.getOrgCode());
        map.put("pageNum", String.valueOf(pageNum));
        map.put("pageSize", String.valueOf(pageSize));
        map.put("key", "123456");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("http://");
        stringBuilder.append(building.getServerIp());
        stringBuilder.append(":");
        stringBuilder.append(building.getServerPort());
        stringBuilder.append("/");
        stringBuilder.append(Constant.initUsers);
        String url = stringBuilder.toString();
        System.out.println("获取初始化员工数据地址：" + url);
        String responseContent = okHttpUtil.post(url, map);
        System.out.println(responseContent);
        InitParentModel initParentModel = JSON.parseObject(responseContent, InitParentModel.class);
        if(initParentModel.getVerify().getSign().equals("fail")){
            logger.error("系统异常");
        }
        totalPage = initParentModel.getData().getTotalPage();
        total = initParentModel.getData().getTotal();
        if(total>0) {
            //companyUserService.delALL();
            for(int i=1;i<=totalPage;i++) {
                map.put("pageNum", String.valueOf(i));
                responseContent = okHttpUtil.post(url, map);
                initParentModel = JSON.parseObject(responseContent, InitParentModel.class);
                List<CompanyUserList> companyUserList2 = initParentModel.getData().getRows();
                for (CompanyUserList companyUser : companyUserList2) {
                    TbCompanyuser companyuser = isCompanyUser(companyUser);
                    if(companyuser == null){
                        continue;
                    }
                    companyuser.save();
                    sendUsers(companyuser);
                }
            }
            renderText("员工初始化接口成功");
        }else {
            renderText("员工初始化接口失败");
        }

    }

    public void sendUsers(TbCompanyuser companyuser) throws Exception {

        String companyfloor = null;
        if (null != companyuser.getCompanyFloor()) {
            companyfloor = companyuser.getCompanyFloor();
        }

        List<String> allFaceDecive = devRelatedService.getAllFaceDeviceIP(companyfloor);
        if (allFaceDecive.size() <= 0 || allFaceDecive == null) {
            System.out.println(companyuser.getUserName()+"无对应设备下发");
            return;
        }
        System.out.println("共需要下发"+allFaceDecive.size()+"台");
        if (allFaceDecive.size() > 0) {
            String issued = "0";
            for (int i = 0; i < allFaceDecive.size(); i++) {
                System.out.println("需下发的人像识别仪器IP为：" + allFaceDecive.get(i));
                TbDevice device = devicesService.findByDeviceIp(allFaceDecive.get(i));
                if (null == device) {
                    System.out.println("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                    continue;
                }
                boolean isSuccess = true;
                if (device.getDeviceType().equals("TPS980")) {

                }else if (device.getDeviceType().equals("DS-K5671")) {
                    if(!Constant.isInitHc) {
                        System.out.println("海康设备SDK初始化失败，无法下发");
                        isSuccess=false;
                        continue;
                    }else {
                        Map<String,String> hcMap =new HashMap<>();
                        hcMap.put("strCardNo", "S"+companyuser.getUserId());
                        hcMap.put("userId", String.valueOf(companyuser.getUserId()));
                        hcMap.put("userName", companyuser.getUserName());
                        String filePath = Constant.StaffPath + companyuser.getUserName() + companyuser.getCompanyId() + ".jpg";
                        hcMap.put("filePath", filePath);
                        hcMap.put("personType", "staff");
                        isSuccess =hcNetSDKService.setCardAndFace(allFaceDecive.get(i), hcMap);
                    }
                }else if(device.getDeviceType().equals("DS-2CD8627FWD")) {

                }else if (device.getDeviceType().equals("DS-K5603-H")) {

                }
                if (isSuccess == false) {
                    issued = "1";
                    TbFailreceive faceReceive = failReceiveService.findOne(allFaceDecive.get(i), companyuser.getUserName(), companyuser.getIdNO(), "staff");
                    if(null == faceReceive) {
                        TbFailreceive failreceive = new TbFailreceive();
                        failreceive.setFaceIp(allFaceDecive.get(i));
                        failreceive.setIdCard(companyuser.getIdNO());
                        failreceive.setUserName(companyuser.getUserName());
                        failreceive.setReceiveFlag("1");
                        failreceive.setUserType("staff");
                        failreceive.setDownNum(0);
                        failreceive.setOpera("save");
                        failreceive.setReceiveTime(getDateTime());
                    }else{
                        int count =faceReceive.getDownNum();
                        count++;
                        faceReceive.setDownNum(count);
                        faceReceive.save();
                    }
                }
                companyuser.setIsSued(issued);
                companyuser.update();
            }
        }
    }
    private TbCompanyuser isCompanyUser(CompanyUserList model) throws Exception {
        if (!"normal".equals(model.getCurrentStatus())) {
           return null;
        }
        TbCompanyuser companyuser = new TbCompanyuser();
        if (model.getPhoto() != null) {
            byte[] photoKey = Base64_2.decode(model.getPhoto());
            String fileName = model.getUserName() + model.getCompanyId() + ".jpg";
            File fileload = FilesUtils.getFileFromBytes(photoKey, Constant.StaffPath, fileName);
            System.out.println("初始化员工存放照片地址"+fileload.getAbsolutePath());
        }else {
            System.out.println(model.getUserName()+"该用户无照片");
            return null;
        }
        companyuser.setIdNO(model.getIdNO());
        companyuser.setCompanyId(model.getCompanyId());
        companyuser.setCompanyFloor(model.getCompanyFloor());
        companyuser.setCurrentStatus(model.getCurrentStatus());
        companyuser.setIdType(model.getIdType());
        companyuser.setUserName(model.getUserName());
        companyuser.setUserId(model.getUserId());
        companyuser.setStatus(model.getStatus());
        companyuser.setReceiveDate(model.getCreateDate());
        companyuser.setReceiveTime(model.getCreateTime());
        companyuser.setRoleType(model.getRoleType());
        companyuser.setIsDel("1");
        companyuser.setIsSued("1");
        return companyuser;
    }

    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
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
}
