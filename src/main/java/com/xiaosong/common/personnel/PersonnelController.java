package com.xiaosong.common.personnel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dhnetsdk.date.Constant;
import com.jfinal.aop.Clear;
import com.jfinal.core.Controller;
import com.jfinal.kit.PathKit;
import com.jfinal.upload.UploadFile;
import com.sun.jna.Pointer;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.wincc.failreceive.FailReceService;
import com.xiaosong.config.InitDevice;
import com.xiaosong.constant.Constants;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.constant.FaceDevResponse;
import com.xiaosong.interceptor.AuthInterceptor;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbDevicerelated;
import com.xiaosong.model.TbFailreceive;
import com.xiaosong.sdkConfig.HCNetSDK;
import com.xiaosong.util.*;
import com.xiaosong.util.QRCodeModel.GetAllStaffModel;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PersonnelController extends Controller {
    public static final String STAFF = "staff";
    private static Logger logger = Logger.getLogger(PersonnelController.class);
    private static PersonnelService srv = PersonnelService.me;
    private static ServerService serSrv = ServerService.me;
    private static DeviceService srvDevice = DeviceService.me;

    OkHttpUtil okHttpUtil = new OkHttpUtil();
//
//    public boolean send(Map<String, String> map, String contralFloor) throws UnsupportedEncodingException {
//        List<TbDevicerelated> devicerelateds = srv.findByDevice(contralFloor);
//        List<TbDevice> devices = new ArrayList<>();
//        boolean isSuccess = true;
//
//        for (TbDevicerelated devicerelated : devicerelateds) {
//            devices.add(srvDevice.findByDeviceIp(devicerelated.getFaceIP()));
//        }
//        if (devices == null || devices.size() == 0) {
//            logger.info("配置的设备没有该对应的通道~");
//            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "配置的设备没有该对应的通道~"));
//        } else {
//
//            for (TbDevice device : devices) {
//                map.put(Constant.deviceIp, device.getDeviceIp());
//
//                if (device.getDeviceType().equals("DH-ASI728")) {
//                    //大华设备人脸下发
//                    int cardInfo = srv.insertInfo(map);
//                    if(!com.xiaosong.constant.Constant.isInitDh){
//                        InitDevice.initHc();
//                    }
//                    if (cardInfo==0) {
//                        logger.info("大华设备添加成功~");
//                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
//                    } else {
//                        logger.error("大华设备添加失败~");
//                        isSuccess=false;
//                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
//                    }
//                } else if (device.getDeviceType().equals("DS-K5671")) {
//                    isSuccess = srv.insertInfoHKGuard(map);
//                    if (isSuccess) {
//                        logger.info("海康门禁添加成功~");
//                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康门禁添加成功~"));
//                    } else {
//                        logger.error("海康门禁添加失败~");
//                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康门禁添加失败"));
//                    }
//                }
//                if (device.getDeviceType().equals("TPS980")) {
//                    //海景设备人脸下发
//                    map.put(Constants.currentStatus, "normal");   //员工状态 海景下发参数
//                    String photo1 = null;
//                    File file = new File(map.get(Constant.photoPath));
//                    photo1 = Base64_2.encode(FilesUtils.getBytesFromFile(file));
//                    isSuccess = srv.sendFaceHJ(map.get(Constant.deviceIp), map, photo1);
//                    if (isSuccess) {
//                        logger.info("海景设备添加成功~");
//                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备添加成功~"));
//                    } else {
//                        logger.error("海景设备添加失败~");
//                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备添加失败~"));
//                    }
//                }
//                if (!isSuccess) {
//                    break;
//                }
//            }
//        }
//        return isSuccess;
//
//    }

    public boolean del(TbCompanyuser companyuser) throws UnsupportedEncodingException {
        List<TbDevicerelated> devicerelateds = srv.findByDevice(companyuser.getCompanyFloor());
        List<TbDevice> devices = new ArrayList<>();
        for (TbDevicerelated devicerelated : devicerelateds) {
            devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
        }
        Map<String, String> m = new HashMap<String, String>();
        boolean del = true;
        if(devices==null || devices.size()<=0){
            return true;
        }
        for (TbDevice device : devices) {
            m.put(Constant.deviceIp, device.getDeviceIp()); //设备ip
            if (device.getDeviceType().equals("DH-ASI728")) {
                //是否初始化过
                if(!com.xiaosong.constant.Constant.isInitDh){
                    InitDevice.initDh();
                }
                //删除大华设备
                Integer cardInfo = companyuser.getCardInfo();
                del = srv.deleteDH(m, cardInfo);
                if (del) {
                    logger.info("大华删除人脸成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华删除人脸成功~"));
                } else {
                    logger.error("大华删除人脸失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华删除人脸失败~"));
                }
            } else if (device.getDeviceType().equals("DS-K5671")) {
                //是否初始化过
                if(!com.xiaosong.constant.Constant.isInitHc){
                    InitDevice.initHc();
                }
                //删除海康设备
                String strCardNo = "S" + companyuser.getUserId();
                del = srv.setCardInfo(device.getDeviceIp(), companyuser.getUserId(),
                        companyuser.getUserName(), strCardNo, "delete");
                if (del) {
                    logger.info("海康设备删除人脸成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康设备删除人脸成功~"));
                } else {
                    logger.error("海康设备删除人脸失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康设备删除人脸失败~"));
                }

            } else if (device.getDeviceType().equals("TPS980")) {
                //删除海景设备
                m.put(Constants.currentStatus, "delete");
                m.put(Constants.idNo, companyuser.getIdNO());
                m.put(Constants.userName, companyuser.getUserName());
                del = srv.deleteFaceHJ(device.getDeviceIp(), m, null);
                if (del) {
                    logger.info("海景设备删除人脸成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备删除人脸成功~"));
                } else {
                    logger.error("海景设备删除人脸失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备删除人脸失败~"));
                }
            }
            if (!del) {
                break;
            }
        }
        return del;
    }

    /**
     * 下发人脸
     */
    public void save() {
        try {
            Map<String, String> map = new HashMap<String, String>();
            UploadFile file1 = getFile();
            String idNO = getPara("idNO");//身份证号码
            String userName = getPara("userName");//用户姓名*
//            String companyId = getPara("companyId");//用户公司id
            String userId = getPara("userId");//用户id
            String contralFloor = getPara("companyFloor");//用户id
            String photo2 = file1.getUploadPath();
            String fileName = file1.getFileName();
            String photo = photo2 + "/" + fileName;

            //照片压缩
            File f = new File(photo);
            String name = f.getName();
            String absolutePath = f.getAbsolutePath();
            String path = f.getParentFile().getPath()+"/";

            String b=Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024*90L));
            byte[] photoKey = Base64_2.decode(b);
            FilesUtils.getFileFromBytes(photoKey, path, name);

            map.put(Constant.photoPath, photo);     //图片*
            map.put(Constant.userId, userId);       //用户id
            map.put(Constant.username, userName);   //用户姓名*
            map.put(Constant.cardNo, idNO);
            TbCompanyuser tc = getModel(TbCompanyuser.class);
            //boolean send = send(map, contralFloor);

            List<TbDevicerelated> devicerelateds = srv.findByDevice(contralFloor);
            List<TbDevice> devices = new ArrayList<>();
            boolean isSuccess = true;

            for (TbDevicerelated devicerelated : devicerelateds) {
                devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
            }
            if (devices == null || devices.size() == 0) {
                logger.info("配置的设备没有该对应的通道~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "配置的设备没有该对应的通道~"));
            } else {
                String issued = "0";

                for (TbDevice device : devices) {
                    map.put(Constant.deviceIp, device.getDeviceIp());

                    if (device.getDeviceType().equals("DH-ASI728")) {
                        //是否初始化过
                        if(!com.xiaosong.constant.Constant.isInitDh){
                            InitDevice.initDh();
                        }
                        //大华设备人脸下发
                        int cardInfo = srv.insertInfo(map);

                        if (cardInfo!=0) {
                            logger.info("大华设备添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
                        } else {
                            isSuccess=false;
                            logger.error("大华设备添加失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
                        }
                    } else if (device.getDeviceType().equals("DS-K5671")) {
                        //是否初始化过
                        if(!com.xiaosong.constant.Constant.isInitHc){
                            InitDevice.initHc();
                        }
                        isSuccess = srv.insertInfoHKGuard(map);
                        if (isSuccess) {
                            logger.info("海康门禁添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康门禁添加成功~"));
                        } else {
                            logger.error("海康门禁添加失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康门禁添加失败"));
                        }
                    }
                    if (device.getDeviceType().equals("TPS980")) {
                        //海景设备人脸下发
                        map.put(Constants.currentStatus, "normal");   //员工状态 海景下发参数
                        String photo1 = null;
                        File file = new File(map.get(Constant.photoPath));
                        photo1 = Base64_2.encode(FilesUtils.getBytesFromFile(file));
                        isSuccess = srv.sendFaceHJ(map.get(Constant.deviceIp), map, photo1);
                        if (isSuccess) {
                            logger.info("海景设备添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备添加成功~"));
                        } else {
                            logger.error("海景设备添加失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备添加失败~"));
                        }
                    }
                    if (isSuccess == false) {
                        issued = "1";
                        TbFailreceive faceReceive = FailReceService.me.findOne(device.getDeviceIp(), userName , "", "staff");

                        if (null == faceReceive) {
                            TbFailreceive newFaceFail = new TbFailreceive();
                            newFaceFail.setFaceIp(device.getDeviceIp());
                            newFaceFail.setIdCard("");
                            newFaceFail.setUserName(userName);
                            newFaceFail.setReceiveFlag(issued);
                            newFaceFail.setUserType("staff");
                            newFaceFail.setDownNum(0);
                            newFaceFail.setOpera("save");
                            newFaceFail.setReceiveTime(getDateTime());
                            newFaceFail.save();
                        } else {
                            int count = faceReceive.getDownNum();
                            count++;
                            faceReceive.setDownNum(count);
                            System.out.println("*****************" + count);
                            faceReceive.update();
                        }
                    }
                }
            }

            if (isSuccess) {

                boolean bool = srv.addData(map, tc, "/img/" + fileName, contralFloor, 0);
                if (bool) {
                    logger.info("添加人员信息成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                } else {
                    logger.error("添加人员信息失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                }
            } else {
                boolean bool = srv.addData(map, tc, "/img/" + fileName, contralFloor, 1);
                if (bool) {
                    logger.info("添加人员信息成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                } else {
                    logger.error("添加人员信息失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("添加人员信息异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息异常~"));
        }
    }

    /**
     * 修改人脸
     */
    public void update() {
        try {
            UploadFile file1 = getFile();
            String id = getPara("id");
            String idNO = getPara("idNO");//身份证号码
            String userName = getPara("userName");//用户姓名*
            String userId = getPara("userId");//用户id
            String contralFloor = getPara("companyFloor");//用户id

            TbCompanyuser companyuser = srv.findByUser(id);
            if (!String.valueOf(companyuser.getUserId()).equals(userId) || !contralFloor.equals(companyuser.getCompanyFloor())) {
                boolean del = del(companyuser);
                if (del) {
                    logger.info("删除信息成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "删除信息成功~"));
                } else {
                    logger.error("删除信息失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除信息失败~"));
                }

            }
            String photo = null;
            String photoName = null;
            if (file1 == null) {
                photo = PathKit.getWebRootPath() + companyuser.getPhoto();
                photoName = companyuser.getPhoto();
            } else {
                String uploadPath = file1.getUploadPath();
                String fileName = file1.getFileName();
                photoName = "/img/" + fileName;

                photo = uploadPath + "/" + fileName;
            }
            //照片压缩
            File f = new File(photo);
            String name = f.getName();
            String absolutePath = f.getAbsolutePath();
            String path = f.getParentFile().getPath()+"/";

            String b=Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024*90L));
            byte[] photoKey = Base64_2.decode(b);
            FilesUtils.getFileFromBytes(photoKey, path, name);

            Map<String, String> map = new HashMap<String, String>();
            map.put(Constant.photoPath, photo);     //图片*
            map.put(Constant.userId, userId);       //用户id
            map.put(Constant.username, userName);   //用户姓名*
            map.put(Constant.cardNo, idNO);
            List<TbDevicerelated> devicerelateds = srv.findByDevice(contralFloor);
            List<TbDevice> devices = new ArrayList<>();
            for (TbDevicerelated devicerelated : devicerelateds) {
                devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
            }
            if (devices == null || devices.size() == 0) {
                companyuser.setCompanyFloor(contralFloor);
                companyuser.update();
                logger.info("配置的设备没有该对应的通道~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "配置的设备没有该对应的通道~"));
            } else {

                boolean isSuccess = true;
                for (TbDevice device : devices) {
                    map.put(Constant.deviceIp, device.getDeviceIp());

                    if (device.getDeviceType().equals("DH-ASI728")) {
                        //是否初始化过
                        if(!com.xiaosong.constant.Constant.isInitDh){
                            InitDevice.initDh();
                        }
                        //大华设备人脸下发
                        int cardInfo = srv.insertInfo(map);

                        if (cardInfo!=0) {
                            logger.info("大华设备添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
                        } else {
                            logger.error("大华设备添加失败~");
                            isSuccess=false;
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
                        }
                    } else if (device.getDeviceType().equals("DS-K5671")) {
                        //是否初始化过
                        if(!com.xiaosong.constant.Constant.isInitHc){
                            InitDevice.initHc();
                        }
                        //海康门禁设备人脸下发
                        isSuccess = srv.insertInfoHKGuard(map);
                        if (isSuccess) {
                            logger.info("海康门禁添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康门禁添加成功~"));
                        } else {
                            logger.error("海康门禁添加失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康门禁添加失败"));
                        }
                    }
                    if (device.getDeviceType().equals("TPS980")) {
                        //海景设备人脸下发
                        map.put(Constants.currentStatus, "normal");   //员工状态 海景下发参数
                        String photo1 = null;
                        File file = new File(map.get(Constant.photoPath));
                        photo1 = Base64_2.encode(FilesUtils.getBytesFromFile(file));
                        isSuccess = srv.sendFaceHJ(map.get(Constant.deviceIp), map, photo1);
                        if (isSuccess) {
                            logger.info("海景设备添加成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备添加成功~"));
                        } else {
                            logger.error("海景设备添加失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备添加失败~"));
                        }
                    }
                    if (!isSuccess) {
                        break;
                    }
                }
                if (isSuccess) {

                    int bool = srv.updateData(map, photoName, contralFloor, Integer.parseInt(id), 0);
                    if (bool == 1) {
                        logger.info("修改人员信息成功~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "修改人员信息成功~"));
                    } else {
                        logger.error("修改人员信息失败~");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "修改人员信息失败~"));
                    }
                } else {
                    int bool = srv.updateData(map, photoName, contralFloor, Integer.parseInt(id), 1);
                    if (bool == 1) {
                        logger.info("修改人员信息成功~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "修改人员信息成功~"));
                    } else {
                        logger.error("修改人员信息失败~");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "修改人员信息失败~"));
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("修改人员信息异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "修改人员信息异常~"));
        }

    }

    /**
     * 删除数据
     */

    public void delete() {
        try {
            //获取前端数据
            String id = getPara("id");
            //根据 用户主键查询 用户所在设备的通行权限
            TbCompanyuser companyuser = srv.findByUser(id);
            if (companyuser != null) {
                //根据 用户所在设备的通行权限 查询设备
                boolean del = true;
                List<TbDevicerelated> devicerelateds = srv.findByDevice(companyuser.getCompanyFloor());
                if (devicerelateds.size() <= 0) {
                    int delete = srv.delete(id);
                    if (delete >= 1) {
                        logger.info("用户删除成功~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "用户删除成功~"));
                    } else {
                        logger.error("用户删除失败~");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户删除失败~"));
                    }
                } else {
                    List<TbDevice> devices = new ArrayList<>();
                    for (TbDevicerelated devicerelated : devicerelateds) {
                        devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
                    }
                    Map<String, String> map = new HashMap<String, String>();
                    for (TbDevice device : devices) {
                        map.put(Constant.deviceIp, device.getDeviceIp()); //设备ip
                        if (device.getDeviceType().equals("DH-ASI728")) {
                            //是否初始化过
                            if(!com.xiaosong.constant.Constant.isInitDh){
                                InitDevice.initDh();
                            }
                            //删除大华设备
                            Integer cardInfo = companyuser.getCardInfo();
                            del = srv.deleteDH(map, cardInfo);
                            if (del) {
                                logger.info("大华删除人脸成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华删除人脸成功~"));
                            } else {
                                logger.error("大华删除人脸失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华删除人脸失败~"));
                            }
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            //是否初始化过
                            if(!com.xiaosong.constant.Constant.isInitHc){
                                InitDevice.initHc();
                            }
                            //删除海康设备
                            String strCardNo = "S" + companyuser.getUserId();
                            del = srv.setCardInfo(device.getDeviceIp(), companyuser.getUserId(),
                                    companyuser.getUserName(), strCardNo, "delete");
                            if (del) {
                                logger.info("海康设备删除人脸成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康设备删除人脸成功~"));
                            } else {
                                logger.error("海康设备删除人脸失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康设备删除人脸失败~"));
                            }

                        } else if (device.getDeviceType().equals("TPS980")) {
                            //删除海景设备
                            map.put(Constants.currentStatus, "delete");
                            map.put(Constants.idNo, companyuser.getIdNO());
                            map.put(Constants.userName, companyuser.getUserName());
                            del = srv.deleteFaceHJ(device.getDeviceIp(), map, null);
                            if (del) {
                                logger.info("海景设备删除人脸成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备删除人脸成功~"));
                            } else {
                                logger.error("海景设备删除人脸失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备删除人脸失败~"));
                            }
                        }
                        if (!del) {
                            break;
                        }
                    }
                    if (del) {
                        int delete = srv.delete(id);
                        if (delete >= 1) {
                            logger.info("用户删除成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "用户删除成功~"));
                        } else {
                            logger.error("用户删除失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户删除失败~"));
                        }
                    }
                }
            } else {
                logger.error("用户不存在~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户不存在~"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("用户删除异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户删除异常~"));
        }
    }

    /**
     * 批量删除人员信息
     */
    public void batchDel() {
        try {
            //获取前端数据
            String id = getPara("id");
            String[] split = id.split(",");
            for (String id1 : split) {
                //根据 用户主键查询 用户所在设备的通行权限
                TbCompanyuser companyuser = srv.findByUser(id1);
                if (companyuser != null) {
                    //根据 用户所在设备的通行权限 查询设备
                    boolean del = true;
                    List<TbDevicerelated> devicerelateds = srv.findByDevice(companyuser.getCompanyFloor());
                    if (devicerelateds.size() <= 0) {
                        int delete = srv.delete(id1);
                        if (delete >= 1) {
                            logger.info("用户批量删除成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "用户批量删除成功~"));
                        } else {
                            logger.error("用户批量删除失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户批量删除失败~"));
                        }
                    } else {
                        List<TbDevice> devices = new ArrayList<>();
                        for (TbDevicerelated devicerelated : devicerelateds) {
                            devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
                        }
                        Map<String, String> map = new HashMap<String, String>();
                        for (TbDevice device : devices) {
                            map.put(Constant.deviceIp, device.getDeviceIp()); //设备ip
                            if (device.getDeviceType().equals("DH-ASI728")) {
                                //是否初始化过
                                if(!com.xiaosong.constant.Constant.isInitDh){
                                    InitDevice.initDh();
                                }
                                //删除大华设备
                                Integer cardInfo = companyuser.getCardInfo();
                                del = srv.deleteDH(map, cardInfo);
                                if (del) {
                                    logger.info("大华删除人脸成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华删除人脸成功~"));
                                } else {
                                    logger.error("大华删除人脸失败~");
                                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华删除人脸失败~"));
                                }
                            } else if (device.getDeviceType().equals("DS-K5671")) {
                                //是否初始化过
                                if(!com.xiaosong.constant.Constant.isInitHc){
                                    InitDevice.initHc();
                                }
                                //删除海康设备
                                String strCardNo = "S" + companyuser.getUserId();
                                del = srv.setCardInfo(device.getDeviceIp(), companyuser.getUserId(),
                                        companyuser.getUserName(), strCardNo, "delete");
                                if (del) {
                                    logger.info("海康设备删除人脸成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康设备删除人脸成功~"));
                                } else {
                                    logger.error("海康设备删除人脸失败~");
                                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康设备删除人脸失败~"));
                                }

                            } else if (device.getDeviceType().equals("TPS980")) {
                                //删除海景设备
                                map.put(Constants.currentStatus, "delete");
                                map.put(Constants.idNo, companyuser.getIdNO());
                                map.put(Constants.userName, companyuser.getUserName());
                                del = srv.deleteFaceHJ(device.getDeviceIp(), map, null);
                                if (del) {
                                    logger.info("海景设备删除人脸成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备删除人脸成功~"));
                                } else {
                                    logger.error("海景设备删除人脸失败~");
                                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备删除人脸失败~"));
                                }
                            }
                            if (!del) {
                                break;
                            }
                        }
                        if (del) {
                            int delete = srv.delete(id1);
                            if (delete >= 1) {
                                logger.info("用户批量删除成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "用户批量删除成功~"));
                            } else {
                                logger.error("用户批量删除失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户批量删除失败~"));
                            }
                        }
                    }
                } else {
                    logger.error("用户不存在~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户不存在~"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("用户批量删除异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户批量删除异常~"));
        }
    }

    /**
     * 条件查询
     */
    public void index() {
        try {
            int page = Integer.parseInt(getPara("currentPage"));
            int number = Integer.parseInt(getPara("pageSize"));
            String userName = getPara("userName");
            String userId = getPara("userId");
            int index = (page - 1) * number;
            List<TbCompanyuser> list = new ArrayList<>();

            List<TbCompanyuser> devicerelateds = null;

            if (userName == null && userId == null) {
                devicerelateds = srv.findCompanyuser();
                for (int i = index; i < devicerelateds.size() && i < (index + number); i++) {
                    list.add(devicerelateds.get(i));
                }
            } else {
                devicerelateds = srv.findCompanyuserbim(userName, userId);
                for (int i = index; i < devicerelateds.size() && i < (index + number); i++) {
                    list.add(devicerelateds.get(i));
                }
            }

                logger.info("员工信息查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, devicerelateds.size()));

        } catch (NumberFormatException e) {
            e.printStackTrace();
            logger.error("员工信息查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "员工信息查询异常"));
        }
    }

    /**
     * 获取图片
     */
    public void pic() {
        try {
            UploadFile photo = getFile();
            System.out.println(photo.getUploadPath());
            if (photo != null) {
                logger.info("图片上传成功,上传的图片名" + photo.getFileName());
                renderJson(RetUtil.ok(photo.getFileName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("图片上传失败");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "图片上传失败~"));
        }
    }


    /**
     * 拉取 服务器 数据 下发设备
     *
     * @throws Exception
     */
    @Clear(AuthInterceptor.class)
    public void pullUser() throws Exception {
        int currentPage = getInt("currentPage");
        int pageNum = 1;
        int pageSize = 10;
        int totalPage = 0;
        int total = 0;
        Map<String, String> map = new HashMap<>();
        map.put("org_code", serSrv.findByOrgCode());
        map.put("pageNum", String.valueOf(pageNum));
        map.put("pageSize", String.valueOf(pageSize));
        map.put("key", "123456");
//        map.put("pageNum", String.valueOf(pageNum));
//        map.put("pageSize", String.valueOf(pageSize));
//        map.put("pospCode", "00000001");
//        map.put("mac", "jLjEwTzY06RFHTbTT8cyfNt+pwJCMzJgFSmO5mKapOLeI41N6wUvvnJ2D6jf76kN7su5LeHgyD70HEGM2K39s46tjTB+vvm0M9eaEUlyJBjykCVAYVt9GUFSrYKu2UTijLUPuQuH8yD/CrIDgZV3gGUT+YMNjwxVccSq4967vGLCYrujmb227R3MiuBuGT4w01+J+NctdWiuPPf4VsyJ5inzAxpWziBqmYaktrAPMqXJ0fdTfC+gsnx9C1FFTzFef00O3TQ/32WbX6tzX8OjCqkTLH4lGN7+cnvtWuMqmuhp0BNJHlXM8pDUlf+Q1iE10REyUV2AT4IdQ7T60MO7ZQ==");

        // stringBuilder.append("http://192.168.10.129:8098/visitor/companyUser/findApplyAllSucOrg");
//        stringBuilder.append(Constants.baseURl);
//        stringBuilder.append(Constants.pullOrgCompanyUrlAll);

        StringBuilder stringBuilder = new StringBuilder();
        String ip = serSrv.findBySerIp();
        String port = serSrv.findBySerPort();
        stringBuilder.append("http://" + ip + ":" + port + "/visitor/companyUser/findApplyAllSucOrg");

        String url = stringBuilder.toString();
        logger.info("获取员工数据地址：" + url);
        // System.out.println("获取员工数据地址：" + url);

        String responseContent = okHttpUtil.post(url, map);
        System.out.println("获取信息" + responseContent);
        GetAllStaffModel allStaffModel = JSON.parseObject(responseContent, GetAllStaffModel.class);
        if(allStaffModel==null){
            logger.error("网络错误...");
            return;
        }
        totalPage = allStaffModel.getData().getTotalPage();
        total = allStaffModel.getData().getTotal();
        if (total > 0) {
            srv.deleteAll();

            for (int i = currentPage; i <= totalPage; i++) {
                map.put("pageNum", String.valueOf(i));
                responseContent = okHttpUtil.post(url, map);
                allStaffModel = JSON.parseObject(responseContent, GetAllStaffModel.class);
                List<TbCompanyuser> companyUserList2 = allStaffModel.getData().getRows();
                for (TbCompanyuser companyUser : companyUserList2) {
                    sendUsers(companyUser);
                }
            }
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "下发成功~"));
            logger.info("下发成功~");
        } else {
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "下发失败~"));
            logger.error("下发失败~");
        }
    }

    // 下发 设备
    private void sendUsers(TbCompanyuser companyUser) throws Exception {

        // 非正常状态员工不接收
        if (!"normal".equals(companyUser.getCurrentStatus())) {
            return;
        }
        companyUser.setIsSued("1");
        companyUser.setIsDel("1");
        companyUser.setReceiveDate(getDate());
        companyUser.setReceiveTime(getTime());

        if (companyUser.getPhoto() != null) {
            //	redisUtils.set("photo_" + companyUser.getCompanyId() + "_" + companyUser.getIdNO(), companyUser.getPhoto());
            byte[] photoKey = Base64_2.decode(companyUser.getPhoto());
            String fileName = companyUser.getUserName() + companyUser.getUserId() + ".jpg";
            File fileload = FilesUtils.getFileFromBytes(photoKey, Constants.StaffPath, fileName);
            logger.info("初始化员工存放照片地址" + fileload.getAbsolutePath());
            companyUser.setPhoto(Constants.StaffPath + fileName);

            //压缩照片
            File test = new File(Constants.StaffPath+fileName);
            String name = test.getName();
            String absolutePath = test.getAbsolutePath();
            String path = test.getParentFile().getPath();

            String b= Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024*90L));
            byte[] photoKey1 = Base64_2.decode(b);
            FilesUtils.getFileFromBytes(photoKey1, Constants.StaffPath, name);

        } else {
            logger.warn(companyUser.getUserName() + "该用户无照片");
            //String keysign = towerInforService.findOrgId() + towerInforService.findPospCode()
            //		+ towerInforService.findKey();
            //logger.sendErrorLog(towerInforService.findOrgId(), companyUser.getUserName() + "该用户无照片", "", "数据错误",
            //		Constants.errorLogUrl, keysign);
            return;
        }
        companyUser.save();

        String companyFloor = null;
        if (null != companyUser.getCompanyFloor()) {
            companyFloor = companyUser.getCompanyFloor();
        }

	/*	String photo = isPhoto(companyUser);
        if (photo == null) {
			return;
		}*/
        List<String> allFaceDecive = srvDevice.getAllFaceDeviceIP(companyFloor);
        System.out.println(allFaceDecive.size());

        if (allFaceDecive.size() <= 0 || allFaceDecive == null) {
            System.out.println("无设备下发");
            return;
        }
        System.out.println("共需要下发" + allFaceDecive.size() + "台");
        if (allFaceDecive.size() > 0) {
            String issued = "0";
            logger.info("需下发的人像识别仪器IP为：" + allFaceDecive.get(0));
            for (int i = 0; i < allFaceDecive.size(); i++) {
                TbDevice device = srvDevice.findByDeviceIp(allFaceDecive.get(i));
                if (null == device) {
                    logger.error("设备表缺少IP为" + allFaceDecive.get(i) + "的设备");
                    continue;
                }
                boolean isSuccess = true;
                //海景设备TPS980
                if (device.getDeviceType().equals("TPS980")) {
                    isSuccess = sendWhiteList((String) allFaceDecive.get(i), companyUser,
                            companyUser.getPhoto());
                }
                //海康门禁设备DS-K5671
                else if (device.getDeviceType().equals("DS-K5671")) {
                    //是否初始化过
                    if(!com.xiaosong.constant.Constant.isInitHc){
                        InitDevice.initHc();
                    }
                    Map<String, String> hcMap = new HashMap<>();
                    hcMap.put(Constant.userId, String.valueOf(companyUser.getUserId()));
                    hcMap.put(Constant.username, companyUser.getUserName());
                    String filePath = Constants.StaffPath + companyUser.getUserName() + companyUser.getUserId() + ".jpg";
                    hcMap.put(Constant.photoPath, filePath);
                    hcMap.put("personType", "staff");
                    hcMap.put(Constant.deviceIp, allFaceDecive.get(i));
                    isSuccess = srv.insertInfoHKGuard(hcMap);

                    //isSuccess = setUser(device, companyUser);
                } else if (device.getDeviceType().equals("DH-ASI728")) {
                    //是否初始化过
                    if(!com.xiaosong.constant.Constant.isInitDh){
                        InitDevice.initDh();
                    }
                    Map<String, String> hcMap = new HashMap<>();
                    hcMap.put("strCardNo", "S" + companyUser.getUserId());
                    hcMap.put(Constant.userId, String.valueOf(companyUser.getUserId()));
                    hcMap.put(Constant.username, companyUser.getUserName());
                    String filePath = Constants.StaffPath + companyUser.getUserName() + companyUser.getUserId() + ".jpg";
                    hcMap.put(Constant.photoPath, filePath);
                    hcMap.put("personType", "staff");
                    hcMap.put(Constant.deviceIp, allFaceDecive.get(i));

                    int cardInfo = srv.insertInfo(hcMap);

                    if (cardInfo!=0) {
                        logger.info("大华设备添加成功~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
                    } else {
                        logger.error("大华设备添加失败~");
                        isSuccess=false;
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
                    }
                }
                // 针对下发失败的需要登记，待下次冲洗下发，已经下发成功的不在下发

                System.out.println(allFaceDecive.get(i));
                if (isSuccess == false) {
                    issued = "1";
                    TbFailreceive faceReceive = FailReceService.me.findOne(allFaceDecive.get(i), companyUser.getUserName(), companyUser.getIdNO(), "staff");

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
                        count++;
                        faceReceive.setDownNum(count);
                        System.out.println("*****************" + count);
                        faceReceive.update();
                    }
                }
            }
            companyUser.setIsSued(issued);
            companyUser.update();
        }
        return;
    }

    /**
     * 获取当前时间 年-月-日  时:分:秒
     *
     * @return
     */
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 获取当前时间 年-月-日
     *
     * @return
     */
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 获取当前时间 时:分:秒
     *
     * @return
     */
    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 海景设备下发
     *
     * @param deviceIp
     * @param visitor
     * @param photo
     * @return
     * @throws Exception
     */
    private boolean sendWhiteList(String deviceIp, TbCompanyuser visitor, String photo) throws Exception {

        JSONObject paramsJson = new JSONObject();
        String URL = "http://" + deviceIp + ":8080/office/addOrDelUser";
        // String option = user.getCurrentStatus().equals("normal") ? "save" : "delete";
        // System.out.println(service.getUserrealname()+"++"+service.getIdNO());
        paramsJson.put("name", visitor.getUserName());
        paramsJson.put("idCard", visitor.getIdNO());
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
        }
        if (thirdResponseObj == null) {
            return false;
        }

        FaceDevResponse faceResponse = JSON.parseObject(thirdResponseObj.getResponseEntity(), FaceDevResponse.class);

        if ("success".equals(thirdResponseObj.getCode())) {
            logger.info(visitor.getUserName() + "下发" + deviceIp + "成功");
        } else {
            logger.error(visitor.getUserName() + "下发" + deviceIp + "失败");
            return false;
        }
        if ("001".equals(faceResponse.getResult())) {
            logger.info("人脸设备接收" + visitor.getUserName() + "成功");
            return true;
        } else {
            logger.error("人脸设备接收" + visitor.getUserName() + "失败，失败原因：" + faceResponse.getMessage());
            return false;
        }
    }

    /**
     * 上传zip 压缩文件
     */
    public void zip() {
        try {
            UploadFile zip = getFile();

            if (zip != null) {
                String fileName = zip.getFileName();
                String split = fileName.split("\\.")[1];
                if (split.equals("zip")) {
                    logger.info("压缩包上传成功,上传的压缩包名" + zip.getFileName());
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, zip.getFileName()));
                } else {
                    logger.error("请选择zip方式 上传 ~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "请选择zip方式 上传 ~"));
                }
            } else {
                logger.error("压缩包上传失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "压缩包上传失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("压缩包上传异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "压缩包上传异常~"));
        }
    }

    /**
     * 批量导入人员
     */
    public void batchImport() {
        try {
            String zipName = getPara("zipName");
            String zipFile = PathKit.getWebRootPath() + "/file/" + zipName;
            File pathFile = new File(PathKit.getWebRootPath() + "/file/");
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            //解决zip文件中有中文目录或者中文文件
            ZipFile zip = new ZipFile(zipFile, Charset.forName("GBK"));
            int id = 11000;
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                InputStream in = zip.getInputStream(entry);
                String outPath = (PathKit.getWebRootPath() + "/file/" + zipEntryName).replaceAll("\\*", "/");

                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));
                if (!file.exists()) {
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                //输出文件路径信息
                //System.out.println(outPath);
                System.out.println(outPath);
                String[] split = outPath.split("/");
                String companyFloor = outPath.split("/")[2];
                System.out.println(companyFloor);

                String name = outPath.split("/")[3].split("\\.")[0];
                String[] split1 = name.split("\\_");
//                String userName = split1[0];
//                String userId = split1[1];


                OutputStream out = new FileOutputStream(outPath);
                byte[] buf1 = new byte[1024];
                int len;
                while ((len = in.read(buf1)) > 0) {
                    out.write(buf1, 0, len);
                }
                in.close();
                out.close();
                Map<String, String> map = new HashMap<String, String>();

                File f = new File(outPath);
                String PhotoName = f.getName();
                String absolutePath = f.getAbsolutePath();
                String path = f.getParentFile().getPath()+"/";

                String b= null;

                byte[] photoKey = new byte[0];
                try {
                    b = Base64_2.encode(FilesUtils.compressUnderSize(FilesUtils.getPhoto(absolutePath), 1024*90L));

                    photoKey = Base64_2.decode(b);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                FilesUtils.getFileFromBytes(photoKey, path, PhotoName);

                map.put(Constant.photoPath, outPath);     //图片*
                map.put(Constant.userId, String.valueOf(id));       //用户id
                map.put(Constant.username, String.valueOf(id));   //用户姓名*
                id++;
                TbCompanyuser tc = getModel(TbCompanyuser.class);
                List<TbDevicerelated> devicerelateds = srv.findByDevice(companyFloor);
                List<TbDevice> devices = new ArrayList<>();
                for (TbDevicerelated devicerelated : devicerelateds) {
                    devices.add(srvDevice.findDeviceIp(devicerelated.getFaceIP()));
                }
                if (devices == null || devices.size() == 0) {
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "配置的设备没有该对应的通道~"));
                } else {

                    boolean isSuccess = true;
                    for (TbDevice device : devices) {
                        map.put(Constant.deviceIp, device.getDeviceIp());

                        if (device.getDeviceType().equals("DH-ASI728")) {
                            //是否初始化过
//                            if(!com.xiaosong.constant.Constant.isInitDh){
//                                InitDevice.initDh();
//                            }
                            //大华设备人脸下发

                            int cardInfo = srv.insertInfo(map);

                            if (cardInfo!=0) {
                                logger.info("大华设备添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
                            } else {
                                logger.error("大华设备添加失败~");
                                isSuccess=false;
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
                            }
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            //是否初始化过
                            if(!com.xiaosong.constant.Constant.isInitHc){
                                InitDevice.initHc();
                            }
                            isSuccess = srv.insertInfoHKGuard(map);
                            if (isSuccess) {
                                logger.info("海康门禁添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康门禁添加成功~"));
                            } else {
                                logger.error("海康门禁添加失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康门禁添加失败"));
                            }
                        }else if (device.getDeviceType().equals("TPS980")) {

                            //海景设备人脸下发
                            map.put(Constants.currentStatus, "normal");   //员工状态 海景下发参数
                            String photo1 = null;
                            File file1 = new File(map.get(Constant.photoPath));
                            photo1 = Base64_2.encode(FilesUtils.getBytesFromFile(file1));
                            isSuccess = srv.sendFaceHJ(map.get(Constant.deviceIp), map, photo1);
                            if (isSuccess) {
                                logger.info("海景设备添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备添加成功~"));
                            } else {
                                logger.error("海景设备添加失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备添加失败~"));
                            }
                        }
                        if (!isSuccess) {
                            TbFailreceive tbFailreceive = new TbFailreceive();
                            tbFailreceive.setFaceIp(device.getDeviceIp());
                            tbFailreceive.setUserName(String.valueOf(id));
                            tbFailreceive.setUserType("staff");
                            tbFailreceive.setReceiveTime(getDateTime());
                            tbFailreceive.setReceiveFlag("1");
                            tbFailreceive.save();

                            break;
                        }
                    }
                    if (isSuccess) {

                        boolean bool = srv.addData(map, tc, outPath, companyFloor, 0);
                        if (bool) {
                            logger.info("添加人员信息成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                        } else {
                            logger.error("添加人员信息失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                        }
                    } else {
                        boolean bool = srv.addData(map, tc, outPath, companyFloor, 1);
                        if (bool) {
                            logger.info("添加人员信息成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                        } else {
                            logger.error("添加人员信息失败~");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                        }
                    }
                }
            }
            System.out.println("******************完毕********************");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设备 初始化
     */
    public void initDevice(){
        try {
            String faceIP = getPara("faceIP");
            String contralFloor = getPara("contralFloor");
            TbDevice device = srvDevice.findByDeviceIp(faceIP);
            String[] split = contralFloor.split("\\|");

            Map<String, String> map = new HashMap<String, String>();

            boolean isSuccess = true;
            for (int i = 1; i < split.length; i++) {
                List<TbCompanyuser> companyusers = srv.findByUserCompanyFloor(split[i]);
                if(companyusers.size()<=0){
                    logger.info("该通道没有人员可下发~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "该通道没有人员可下发~"));
                }else{
                    for (TbCompanyuser companyuser : companyusers) {

                        map.put(Constant.deviceIp, faceIP);
                        map.put(Constant.photoPath, PathKit.getWebRootPath()+companyuser.getPhoto());     //图片*
                        map.put(Constant.userId, String.valueOf(companyuser.getUserId()));       //用户id
                        map.put(Constant.username, companyuser.getUserName());   //用户姓名*
                        if (device.getDeviceType().equals("DH-ASI728")) {
                            //大华设备人脸下发

                            int cardInfo = srv.insertInfo(map);

                            if (cardInfo==0) {
                                logger.info("大华设备添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "大华设备添加成功~"));
                            } else {
                                logger.error("大华设备添加失败~");
                                isSuccess=false;
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "大华设备添加失败~"));
                            }
                        } else if (device.getDeviceType().equals("DS-K5671")) {
                            //海康门禁设备人脸下发
                            //linux 初始化 海康设备
                            //                        InitHCNetSDK.run(deviceType);
                            //winds 初始化海康设备
                            //                        devicesInit.initHC();
                            isSuccess = srv.insertInfoHKGuard(map);
                            if (isSuccess) {
                                logger.info("海康门禁添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海康门禁添加成功~"));
                            } else {
                                logger.error("海康门禁添加失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海康门禁添加失败"));
                            }
                        }
                        if (device.getDeviceType().equals("TPS980")) {
                            //海景设备人脸下发
                            map.put(Constants.currentStatus, "normal");   //员工状态 海景下发参数
                            String photo1 = null;
                            File file1 = new File(map.get(Constant.photoPath));
                            photo1 = Base64_2.encode(FilesUtils.getBytesFromFile(file1));
                            isSuccess = srv.sendFaceHJ(map.get(Constant.deviceIp), map, photo1);
                            if (isSuccess) {
                                logger.info("海景设备添加成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "海景设备添加成功~"));
                            } else {
                                logger.error("海景设备添加失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "海景设备添加失败~"));
                            }
                        }
                        //if (!isSuccess) {
                        //    break;
                        //}
                        if (isSuccess) {
                            companyuser.setIsSued("0");
                            companyuser.setReceiveDate(getDate());
                            companyuser.setReceiveTime(getDateTime());
                            boolean update = companyuser.update();
                            if (update) {
                                logger.info("添加人员信息成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                            } else {
                                logger.error("添加人员信息失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                            }
                        } else {
                            companyuser.setIsSued("0");
                            companyuser.setReceiveDate(getDate());
                            companyuser.setReceiveTime(getDateTime());
                            boolean update = companyuser.update();
                            if (update) {
                                logger.info("添加人员信息成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加人员信息成功~"));
                            } else {
                                logger.error("添加人员信息失败~");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加人员信息失败~"));
                            }
                        }
                    }

                }

            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public String[] chars = new String[] {  "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

    /**
     * 生成8位UUId
     *
     * @return
     */
    public  String generateUuid8() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 5; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % 0x3E]);
        }
        return shortBuffer.toString();

    }


    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    static int lUserID;// 用户句柄
    static FRemoteCfgCallBackFaceGet fRemoteCfgCallBackFaceGet = null;

    public void detection() {
        // TODO Auto-generated method stub
        load();
        String deviceIp = getPara("deviceIp");
        String userId = getPara("userId");

        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true) {
            System.out.println("初始化失败");
        }
        //设备IP
        String s[] = {deviceIp};
        for(int k =0;k<s.length;k++) {
            //String m_sDeviceIP = s[k];
            String m_sDeviceIP = s[k];
            HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();// 设备信息
            int iPort = 8000;
            lUserID = hCNetSDK.NET_DVR_Login_V30(m_sDeviceIP, (short) iPort, "admin", "wgmhao123", m_strDeviceInfo);
            System.out.println(s[k]+"===:"+lUserID);

            HCNetSDK.NET_DVR_FACE_PARAM_COND m_struFaceInputParam = new HCNetSDK.NET_DVR_FACE_PARAM_COND();
            m_struFaceInputParam.dwSize = m_struFaceInputParam.size();
//卡号
            String strCardNo = "S"+userId;// 人脸关联的卡号
            System.out.println("卡号："+strCardNo);
            for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
                m_struFaceInputParam.byCardNo[i] = 0;
            }
            for (int i = 0; i < strCardNo.length(); i++) {
                m_struFaceInputParam.byCardNo[i] = strCardNo.getBytes()[i];
            }

            m_struFaceInputParam.byEnableCardReader[0] = 1;
            m_struFaceInputParam.dwFaceNum = 1;
            m_struFaceInputParam.byFaceID = 1;
            m_struFaceInputParam.write();

            Pointer lpInBuffer = m_struFaceInputParam.getPointer();
            Pointer pUserData = null;
            fRemoteCfgCallBackFaceGet = new FRemoteCfgCallBackFaceGet();

            int lHandle = hCNetSDK.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_GET_FACE_PARAM_CFG, lpInBuffer,
                    m_struFaceInputParam.size(), fRemoteCfgCallBackFaceGet, pUserData);
            try {
                new Thread().sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static class FRemoteCfgCallBackFaceGet implements HCNetSDK.FRemoteConfigCallback {
        public void invoke(int dwType, Pointer lpBuffer, int dwBufLen, Pointer pUserData) {
            System.out.println("长连接回调获取数据,NET_SDK_CALLBACK_TYPE_STATUS:" + dwType);
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
                            System.out.println("查询人脸参数成功,dwStatus:" + iStatus);
                            break;
                        case 1001:
                            byCardNo = new byte[32];
                            System.arraycopy(struCallbackStatus.byValue, 4, byCardNo, 0, 32);
                            System.out.println("正在查询人脸参数中,dwStatus:" + iStatus + ",卡号:" + new String(byCardNo).trim());
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
                            System.out.println("查询人脸参数失败, dwStatus:" + iStatus + ",错误号:" + iErrorCode + ",卡号:"
                                    + new String(byCardNo).trim());
                            break;
                    }
                    break;
                case 2: // NET_SDK_CALLBACK_TYPE_DATA
                    HCNetSDK.NET_DVR_FACE_PARAM_CFG m_struFaceInfo = new HCNetSDK.NET_DVR_FACE_PARAM_CFG();
                    m_struFaceInfo.write();
                    Pointer pInfoV30 = m_struFaceInfo.getPointer();
                    pInfoV30.write(0, lpBuffer.getByteArray(0, m_struFaceInfo.size()), 0, m_struFaceInfo.size());
                    m_struFaceInfo.read();
                    String str = new String(m_struFaceInfo.byCardNo).trim();
                    System.out.println("查询到人脸数据关联的卡号,getCardNo:" + str + ",人脸数据类型:" + m_struFaceInfo.byFaceDataType);
                    if (m_struFaceInfo.dwFaceLen > 0) {
                        System.out.println(str+"存在人脸");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static void load() {
        String strPathCom2 = "/usr/apache-tomcat-8088-jar/lib/";
        HCNetSDK.NET_DVR_LOCAL_SDK_PATH struComPath2 = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
        System.arraycopy(strPathCom2.getBytes(), 0, struComPath2.sPath, 0, strPathCom2.length());
        struComPath2.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(2, struComPath2.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto = "/usr/apache-tomcat-8088-jar/lib/libssl.so";
        System.arraycopy(strPathCrypto.getBytes(), 0, ptrByteArrayCrypto.byValue, 0, strPathCrypto.length());
        ptrByteArrayCrypto.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto2 = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto2 = "/usr/apache-tomcat-8088-jar/lib/libcrypto.so.1.0.0";
        System.arraycopy(strPathCrypto2.getBytes(), 0, ptrByteArrayCrypto2.byValue, 0, strPathCrypto2.length());
        ptrByteArrayCrypto2.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto2.getPointer());

        HCNetSDK.BYTE_ARRAY ptrByteArrayCrypto3 = new HCNetSDK.BYTE_ARRAY(256);
        String strPathCrypto3 = "/usr/apache-tomcat-8088-jar/lib/libcrypto.so";
        System.arraycopy(strPathCrypto3.getBytes(), 0, ptrByteArrayCrypto3.byValue, 0, strPathCrypto3.length());
        ptrByteArrayCrypto3.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto3.getPointer());
    }
}