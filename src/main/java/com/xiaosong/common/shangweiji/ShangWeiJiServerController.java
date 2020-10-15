package com.xiaosong.common.shangweiji;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Clear;
import com.jfinal.core.Controller;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;
import com.jfinal.upload.UploadFile;
import com.xiaosong.common.accessrecord.AccessRecordService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.websocket.WebSocket;
import com.xiaosong.constant.Constants;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.interceptor.AuthInterceptor;
import com.xiaosong.model.*;
import com.xiaosong.util.*;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Clear(AuthInterceptor.class)
public class ShangWeiJiServerController extends Controller {
    private static Logger logger = Logger.getLogger(ShangWeiJiServerController.class);
    private static ShangWeiJiServerService srv = ShangWeiJiServerService.me;
    Cache redisUtils = Redis.use("xiaosong");
    private String TotalPages = "1";
    private CheckUtils checkUtils = new CheckUtils();
    private OkHttpUtil okHttpUtil = new OkHttpUtil();

    private WebSocket webSocket = new WebSocket();


    /**
     * 防疫二维码
     */
    public void getScanByNetIP() {
        try {

            String qrCodeIp = null;
            BufferedReader reader = null;
            try {
                HttpServletRequest request = getRequest();
                qrCodeIp = IPUtil.getIp(request);
                System.out.println("MX86扫描到二维码数据");
                reader = request.getReader();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            if (reader == null) {
                logger.error("二维码数据解析失败");
            } else {
                char[] buf = new char[1024];
                int len = 0;
                StringBuffer contentBuffer = new StringBuffer();
                try {
                    while ((len = reader.read(buf)) != -1) {
                        contentBuffer.append(buf, 0, len);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // 二维码读取到的数据
                TbDevicerelated tbDevicerelated = srv.findByQRCodeIP(qrCodeIp);
                String content = contentBuffer.toString();
                logger.info(qrCodeIp + "返回二维码数据：" + content);
                Map<String, String> map = new HashMap<>();
                map.put("url", content);

                String response = null;
                try {
                    response = okHttpUtil.post(Constants.baseURl + Constants.qrcode, map);
                } catch (Exception e) {
                    // TODO Auto-generated catch block

                    e.printStackTrace();
                }
                logger.info("服务端内容返回：" + response);
                if (null == response) {
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "服务器错误"));
                }
                //查询 大楼 信息
                TbBuildingServer buildingServer = srv.findFloor();
                JSONObject parse = JSON.parseObject(response);
                JSONObject verify = JSON.parseObject(parse.getString("verify"));
                JSONObject date = JSON.parseObject(parse.getString("data"));
                if (verify.getString("sign").equals("success")) {
                    //所有通行方式
                    if (buildingServer.getQrcodeType().equals("all")) {
                        Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                        saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                        logger.info("所有通行方式_开门成功~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "所有通行方式_开门成功~"));
                    }
                    //禁止通行方式
                    else if (buildingServer.getQrcodeType().equals("stop")) {
                        logger.info("二维码禁止通行~");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                    }
                    //访客通行方式
                    else if (buildingServer.getQrcodeType().equals("visitor")) {
                        String name = (String) date.get("name");
                        String date1 = getDateT();

                        TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                        //根据姓名 和访问时间 判断是否有这个访客
                        if (tbVisitor != null) {
                            Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                            saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            logger.info("访客通行方式_开门成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                        } else {
                            TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                            if (tbCompanyuser != null) {
                                Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                logger.info("员工通行方式_开门成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                            } else {
                                logger.info("无权限开门~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                            }
                        }
                    } else {
                        String timeT = getTimeT();
                        //全员通行 时间段
                        if (srv.findAllDate(timeT) != null) {
                            Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                            saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            logger.info("全部通行_开门成功~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "全部通行_开门成功~"));
                        }
                        //访客通行 时间段
                        else if (srv.findVisitorDate(timeT) != null) {
                            String name = (String) date.get("name");
                            String date1 = getDateT();
                            TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                            //根据姓名 和访问时间 判断是否有这个访客
                            if (tbVisitor != null) {
                                Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                logger.info("访客通行方式_开门成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                            } else {
                                TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                                if (tbCompanyuser != null) {
                                    Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                    saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                    logger.info("员工通行方式_开门成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                                } else {
                                    logger.info("无权限开门~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                                }
                            }

                        }
                        //禁止通行 时间段
                        else if (srv.findStopDate(timeT) != null) {
                            logger.info("二维码禁止通行~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                        }
                    }

                } else {
                    logger.error("开门失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "开门失败~"));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("开门失败~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "开门异常~"));
        }
    }


    public static void main(String[] args) throws ParseException {
//        String s="V817";
        String s2 = "vgdecoderesult=http://hb2.doone.com.cn/b?u=cdFAkwCPBGNclhGH&s=d4UQmeZa88V6&&devicenumber=0\n";
        System.out.println(s2.substring(15, 74));
//        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);//获取到4102当前的小时
//        System.out.println(hour);
//        if (hour == 0 || hour == 1 || hour == 2 || hour == 3 || hour == 4 || hour == 5 || hour == 6 || hour == 7 ) {
//            System.out.println("现在1653是版12点以前权");
//        } else {
//            System.out.println("现在是12点以后");
//        }
    }

    public void scanMX86() {
        try {
            HttpServletRequest request = getRequest();
            String qrCodeIp = IPUtil.getIp(request);
            logger.info("MX86扫描到二维码数据");
            BufferedReader reader = request.getReader();
            if (reader == null) {
                logger.error("二维码数据解析失败");
            } else {
                char[] buf = new char[1024];
                int len = 0;
                StringBuffer contentBuffer = new StringBuffer();
                while ((len = reader.read(buf)) != -1) {
                    contentBuffer.append(buf, 0, len);
                }
                // 二维码读取到的数据
                String content = contentBuffer.toString();
                // https 请求
//                System.out.println("vgdecoderesult___" + content.indexOf("vgdecoderesult="));
//                int vlen = "vgdecoderesult=".length();
//                int i = content.indexOf("&&devicenumber");
//
//                System.out.println(content.substring(content.indexOf("vgdecoderesult=") + vlen, i));
//                content = content.substring(content.indexOf("vgdecoderesult=") + vlen, i);
                // 数组分两部分，公共标题+主内容
                String splitStrings[] = content.split("\\|");
                // 公共标题数组
                String qrCodeCommonMessage = splitStrings[0].trim();
                String[] commonMessageSplit = qrCodeCommonMessage.split("\\&");
                if(content.equals("https://u.wechat.com/MMTxGUxfjmtz6RhYSKHXmZY")){
                    renderText("code=0000");
                    logger.info("____________________________");
                    return;
                }
                if (splitStrings.length != 2 || commonMessageSplit.length != 5) {
                    //防疫二维码
                    // 二维码读取到的数据
                    TbDevicerelated tbDevicerelated = srv.findByQRCodeIP(qrCodeIp);
                    logger.info(qrCodeIp + "返回二维码数据：" + content);
                    Map<String, String> map = new HashMap<>();
                    map.put("url", content);

                    String response = null;
                    try {
                        response = okHttpUtil.post(Constants.baseURl + Constants.qrcode, map);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block

                        e.printStackTrace();
                    }
                    logger.info("服务端内容返回：" + response);
                    if (null == response) {
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "服务器错误"));
                    }
                    //查询 大楼 信息
                    TbBuildingServer buildingServer = srv.findFloor();
                    JSONObject parse = JSON.parseObject(response);
                    JSONObject verify = JSON.parseObject(parse.getString("verify"));
                    JSONObject date = JSON.parseObject(parse.getString("data"));
                    if (verify.getString("sign").equals("success")) {
                        //所有通行方式
                        if (buildingServer.getQrcodeType().equals("all")) {
                            if (tbDevicerelated.getRelayIP() != null) {
                                Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                logger.info("所有通行方式_开门成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "所有通行方式_开门成功~"));
                            } else {
                                logger.info("所有通行方式_开门成功~");
                                renderText("code=0000");
                            }
                            saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());

                        }
                        //禁止通行方式
                        else if (buildingServer.getQrcodeType().equals("stop")) {
                            logger.info("二维码禁止通行~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                        }
                        //访客通行方式
                        else if (buildingServer.getQrcodeType().equals("visitor")) {
                            String name = (String) date.get("name");
                            String date1 = getDateT();

                            TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                            //根据姓名 和访问时间 判断是否有这个访客
                            if (tbVisitor != null) {
                                if (tbDevicerelated.getRelayIP() != null) {
                                    Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                    logger.info("访客通行方式_开门成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                                } else {
                                    logger.info("访客通行方式_开门成功~");
                                    renderText("code=0000");
                                }
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            } else {
                                TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                                if (tbCompanyuser != null) {

                                    if (tbDevicerelated.getRelayIP() != null) {
                                        Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                        logger.info("员工通行方式_开门成功~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                                    } else {
                                        logger.info("员工通行方式_开门成功~");
                                        renderText("code=0000");
                                    }
                                    saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                } else {
                                    logger.info("无权限开门~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                                }
                            }
                        } else {
                            String timeT = getTimeT();
                            //全员通行 时间段
                            if (srv.findAllDate(timeT) != null) {
                                if (tbDevicerelated.getRelayIP() != null) {
                                    Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                    logger.info("全部通行_开门成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "全部通行_开门成功~"));
                                } else {
                                    logger.info("全部通行_开门成功~");
                                    renderText("code=0000");
                                }
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            }
                            //访客通行 时间段
                            else if (srv.findVisitorDate(timeT) != null) {
                                String name = (String) date.get("name");
                                String date1 = getDateT();
                                TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                                //根据姓名 和访问时间 判断是否有这个访客
                                if (tbVisitor != null) {
                                    if (tbDevicerelated.getRelayIP() != null) {
                                        Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                        logger.info("访客通行方式_开门成功~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                                    } else {
                                        logger.info("访客通行方式_开门成功~");
                                        renderText("code=0000");
                                    }
                                    saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                } else {
                                    TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                                    if (tbCompanyuser != null) {
                                        if (tbDevicerelated.getRelayIP() != null) {
                                            Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                            logger.info("员工通行方式_开门成功~");
                                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                                        } else {
                                            logger.info("员工通行方式_开门成功~");
                                            renderText("code=0000");
                                        }
                                        saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                    } else {
                                        logger.info("无权限开门~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                                    }
                                }

                            }
                            //禁止通行 时间段
                            else if (srv.findStopDate(timeT) != null) {
                                logger.info("二维码禁止通行~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                            }
                        }

                    } else {
                        logger.error("开门失败~");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "开门失败~"));
                    }

                } else {
                    System.out.println("二维码类型:" + commonMessageSplit[1]);
                    if (commonMessageSplit[1].equals("4") || commonMessageSplit[1].equals("5")) {
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);
                        int recirdId = Integer.valueOf(contentStringLists.get(1));

                        TbShareroom shareroom = srv.findByRecordId(recirdId);
                        if (null == shareroom) {
                            logger.error("该二维码数据不存在");
                            return;
                        }
                        if (!shareroom.getApplyDate().equals(contentStringLists.get(2))
                                || !shareroom.getApplyStartTime().equals(contentStringLists.get(3))
                                || !shareroom.getApplyEndTime().equals(contentStringLists.get(4))) {
                            logger.error("二维码时间数据与本地时间数据不一致");
                            renderNull();
                            return;
                        }

                        if (shareroom.getRecordStatus() == 1 || shareroom.getRecordStatus() == 4) {
                            logger.error("订单状态无效");
                            renderNull();
                            return;
                        }

                        TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);

                        if (null == devRelated) {
                            logger.error("数据库中未找到二维码读头IP地址");
                            renderNull();
                            return;
                        }
                        if (devRelated.getTurnOver().equals("out")) {
                            open(devRelated, shareroom.getUserName(), shareroom.getIdNo(), "applicant");
                        }
                        if (!devRelated.getContralFloor().contains(shareroom.getRoomAddr())) {
                            logger.error("二维码访问楼层错误");
                            renderNull();
                            return;
                        }
                        // 判断时间是否在有效期
                        // System.out.println(Misc.compareDate2(shareroom.getApply_date(), getDate()));
                        if (Misc.compareDate2(shareroom.getApplyDate(), getDate())) {
                            int startSecond = (int) (Float.parseFloat(shareroom.getApplyStartTime()) * 3600);
                            int endSecond = (int) (Float.parseFloat(shareroom.getApplyEndTime()) * 3600);

                            if (getSecond() > startSecond && getSecond() < endSecond) {
                                String key = shareroom.getUserName() + "_" + shareroom.getIdNo();
                                if (null == redisUtils.get(key)) {
                                    open(devRelated, shareroom.getUserName(), shareroom.getIdNo(), "applicant");
                                    redisUtils.set(key, "locaked");
                                    redisUtils.expire(key, 2);
                                }

                            } else {
                                logger.error("已过访问有效期");
                                renderNull();
                                return;
                            }
                        } else {
                            logger.error("已过访问有效期");
                            renderNull();
                            return;
                        }
                    } else if (commonMessageSplit[1].equals("1")) {
                        //门禁二维码
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);

                        String userName = contentStringLists.get(0);
                        String companyId = contentStringLists.get(1);
                        userId = contentStringLists.get(2);

                        TbCompanyuser user = srv.findByUserId(Integer.valueOf(userId));
                        if (null == user) {
                            logger.error("用户所在公司不在该大楼");
                            renderNull();
                            return;
                        }
                        if (!user.getUserName().equals(userName)) {
                            logger.error("二维码数据错误，名字不匹配");
                            renderNull();
                        } else {
                            TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);
                            open(devRelated, user.getUserName(), user.getIdNO(), "staff");
                            renderText("code=0000");
                        }
                    } else if (commonMessageSplit[1].equals("2")) {
                        //门禁二维码
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);

                        String userName = contentStringLists.get(0);
                        userId = contentStringLists.get(1);
                        startDateTime = contentStringLists.get(2);
                        endDateTime = contentStringLists.get(3);

                        TbVisitor visitor = srv.findByVisitorId(Integer.valueOf(userId));
                        if (null == visitor) {
                            logger.error("用户所在公司不在该大楼");
                            renderNull();
                            return;
                        }
                        if (!visitor.getVisitorName().equals(userName)) {
                            logger.error("二维码数据错误，名字不匹配");
                            renderNull();
                        } else {
                            TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);
                            TbVisitor tbVisitor = srv.findByVisitDate(startDateTime, endDateTime);
                            if (tbVisitor == null) {
                                logger.info("访客不在在有效期, 禁止开门");
                                renderNull();
                            } else {
                                open(devRelated, visitor.getVisitorName(), visitor.getVisitorIdCard(), "staff");
                                renderText("code=0000");
                            }


                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("二维码异常");

        }
    }

    private static String userId = null;
    private static String startDateTime = null;
    private static String endDateTime = null;

    /*
    * 解析“[]”内容
    *
    * SoleCode(contentStringLists.get(0)) //唯一身份识别码
    * RealName(contentStringLists.get(1)) //访客姓名 IdNO(contentStringLists.get(2))
    * //访客证件号 Province(contentStringLists.get(3)) //访问的省
    * City(contentStringLists.get(4)) //访问的市
    * VisitorCompany(contentStringLists.get(5)) //被访问者公司名字
    * VisitorName(contentStringLists.get(6)) //被访问者名字
    * Phone(contentStringLists.get(7)) //访问者手机号
    * HeadImgUrl(contentStringLists.get(8)) //访问者照片(目前不存照片)
    * StarDate(contentStringLists.get(9)) //访问开始时间
    * EndDate(contentStringLists.get(10)) //访问结束时间
    * UserCompanyId(contentStringLists.get(11)) //访问者的公司ID
    * UserCompanyName(contentStringLists.get(12)) //访问者的公司名字
    */
    public static List<String> parseContent(String content) {
        List<String> ls = new ArrayList<String>();
        Pattern pattern = Pattern.compile("(\\[[^\\]]*\\])");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String s = matcher.group();
            if (s.length() > 2) {
                s = s.substring(1, s.length() - 1);
            } else {
                s = "";
            }
            ls.add(s);
        }
        return ls;
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

    private String getTimeT() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    private String getDateT() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }


    // 返回当前时间秒
    private int getSecond() {
        Calendar now = Calendar.getInstance();
        int day = Integer.valueOf(now.get(Calendar.HOUR_OF_DAY));
        int minute = Integer.valueOf(now.get(Calendar.MINUTE));
        int second = Integer.valueOf(now.get(Calendar.SECOND));
        int nowSecond = day * 3600 + minute * 60 + second;
        return nowSecond;
    }


    private void open(TbDevicerelated devRelated, String name, String idCard, String type) throws Exception {

        if (devRelated.getRelayIP() != null) {
            logger.info("继电器开始开门...");
            Control24DeviceUtil.controlDevice(devRelated.getRelayIP(), 8080, devRelated.getRelayOUT(),
                    ServerService.me.findByOrgCode());
        } else {
            logger.info("二维码继电器开始开门...");
//            renderText("code=0000");
        }
        saverecord(name, idCard, type, devRelated.getQRCodeIP(), devRelated.getRelayOUT());

    }

    private void saverecord(String name, String idCard, String personType, String faceIP, String OUT) {
        // TODO Auto-generated method stub
        TbAccessrecord accessRecord = new TbAccessrecord();
        accessRecord.setOrgCode(ServerService.me.findByOrgCode());
        accessRecord.setPospCode(ServerService.me.findPospCode());
        accessRecord.setScanDate(getDate());
        accessRecord.setScanTime(getTime());
        accessRecord.setDeviceType("QRCODE");
        accessRecord.setDeviceIp(faceIP);
        accessRecord.setUserType(personType);
        accessRecord.setUserName(name);
        accessRecord.setIdCard(idCard);
        accessRecord.setTurnOver(OUT);
        accessRecord.setIsSendFlag("F");
        accessRecord.setCardNO(userId);
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
        jo.put("scanDate", getDate() + " " + getDate());
        jo.put("turnOver", OUT);
        jo.put("deviceIp", faceIP);
        jo.put("userName", NameUtils.AccordingToName(name));
        jo.put("userType", personType);
        jo.put("deviceType", "QRCODE");

        jo.put("face", face);
        jo.put("qrCode", qrCode);
        jo.put("staff", staff);
        jo.put("visitor", visitor);
        list.add(jo);
        String str = JSONObject.toJSONString(list);
        webSocket.onMessage(str);

    }

    /**
     * 海景设备 拉取通行人员
     *
     * @throws Exception
     */
    public void sendCamRes() throws Exception {
        try {
            UploadFile file = getFile();

            String facecomparescope = getPara("facecomparescope");
            String name = getPara("name");
            String idCard = getPara("idCard");
            String type = getPara("type");
            String faceRecogIp = IPUtil.getIp(getRequest());
            logger.info(name + "***" + type + "***" + faceRecogIp + "***" + facecomparescope);

            if (StringUtils.isEmpty(type)) {
                logger.error(type);
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "人员类型参数缺失"));
                return;
            }
            if (StringUtils.isEmpty(faceRecogIp)) {
                logger.error(faceRecogIp);
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "比对值识别器IP地址缺失"));
                return;
            }
            if (StringUtils.isEmpty(facecomparescope)) {
                logger.error(facecomparescope);
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "比对值参数缺失"));
                return;
            }
            if (StringUtils.isEmpty(name)) {
                logger.error(name);
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "名字缺失"));
            }
            if (StringUtils.isEmpty(idCard)) {
                logger.error(idCard);
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "身份号码缺失"));
                return;
            }
            TbDevice device = srv.findByDeviceIp(faceRecogIp);
            TbDevicerelated devRelated = TbDevicerelated.dao.findFirst("select * from tb_devicerelated where faceIP = ?", faceRecogIp);
            if (null == device) {
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "失败"));
                return;
            }

            if (type.equals("visitor")) {

                if (devRelated.getTurnOver().equals("out")) {
                    open(devRelated, name, idCard, type, "");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "成功"));
                } else {
                    // 获取访客有无访问数据
                    List<TbVisitor> staffs = srv.findByBetweenTime(name, idCard, getDateTime());
                    if (staffs.size() > 0) {
                        open(devRelated, name, idCard, type, "");
                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "成功"));

                    } else {
                        logger.error("该访客访问时间过期，访问无效");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "访问无效"));
                    }
                }
            } else {
                open(devRelated, name, idCard, type, "");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "成功"));

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("访问错误~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "访问错误~"));
        }
    }

    // 开门并记录通行
    private void open(TbDevicerelated devRelated, String name, String idCard, String type, String cardNo) throws Exception {


        Control24DeviceUtil.controlDevice(devRelated.getRelayIP(), 8080, devRelated.getRelayOUT(),
                null);
        saverecord(name, idCard, type, devRelated.getFaceIP(), devRelated.getRelayOUT(), cardNo);
    }

    /**
     * 保存 通行记录
     *
     * @param name       通行人员名称
     * @param idCard     通行人员身份证号码
     * @param personType 通行人员类型
     * @param faceIP     设备ip
     * @param OUT        继电器输出口
     */
    public void saverecord(String name, String idCard, String personType, String faceIP, String OUT, String card) {
        // TODO Auto-generated method stub
        TbAccessrecord accessRecord = getModel(TbAccessrecord.class);
        accessRecord.setOrgCode(ServerService.me.findByOrgCode());
        accessRecord.setPospCode(ServerService.me.findPospCode());
        accessRecord.setScanDate(getDate());
        accessRecord.setScanTime(getDate());
        accessRecord.setDeviceType("FACE");
        accessRecord.setDeviceIp(faceIP);
        accessRecord.setUserType(personType);
        accessRecord.setUserName(name);
        accessRecord.setIdCard(idCard);
        accessRecord.setTurnOver(OUT);
        accessRecord.setIsSendFlag("F");
        accessRecord.save();

        List<Object> list = new ArrayList<>();
        JSONObject jo = new JSONObject();
        jo.put("scanDate", getDate() + " " + getDate());
        jo.put("turnOver", OUT);
        jo.put("deviceIp", faceIP);
        jo.put("userName", NameUtils.AccordingToName(name));
        jo.put("userType", personType);
        jo.put("deviceType", "FACE");
        list.add(jo);

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
        jo.put("face", face);
        jo.put("qrCode", qrCode);
        jo.put("staff", staff);
        jo.put("visitor", visitor);
        list.add(jo);
        String str = JSONObject.toJSONString(list);


        webSocket.onMessage(str);
    }


    /**
     * 海景 二维码
     *
     * @throws Exception
     */
    public void scanS() throws Exception {

        try {
            String content = getPara("content");
            System.out.println(content);
            String faceRecogIp = IPUtil.getIp(getRequest());
            // 读取扫描二维码数据

            String splitStrings[] = content.split("\\|");
            String commonMessage = splitStrings[0].trim();
            String[] commonMessageSplit = commonMessage.split("\\&");

            if (splitStrings.length != 2 || commonMessageSplit.length != 5) {
                logger.error("二维码扫描数据错误");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "二维码扫描数据错误"));
            }
            if (!commonMessageSplit[3].equals(TotalPages)) {
                logger.error("二维码扫描数据错误");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "二维码扫描数据错误"));
            }

            // 解析头部分内容
            QRCodeCommonMessage qrCodeCommonMessage = new QRCodeCommonMessage();
            qrCodeCommonMessage.setIdentifier(commonMessageSplit[0]);
            qrCodeCommonMessage.setBitMapType(commonMessageSplit[1]);
            qrCodeCommonMessage.setCurrentPage(commonMessageSplit[2]);
            qrCodeCommonMessage.setTotalPages(commonMessageSplit[3]);
            qrCodeCommonMessage.setViewTime(commonMessageSplit[4]);

            // BASE64加密内容解析
            String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
            logger.info("BASE64加密内容解析内容:" + strByte);
            List<String> contentStringLists = parseContent(strByte);
            System.out.println(contentStringLists.toString());
            if (qrCodeCommonMessage.getBitMapType().equals("1")) {
                String userName = contentStringLists.get(0);
                String companyId = contentStringLists.get(1);
                String userId = contentStringLists.get(2);
                if (userName.isEmpty() || companyId.isEmpty() || userId.isEmpty()) {
                    logger.error("二维码数据空");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "二维码数据空"));
                }
                TbCompanyuser user = srv.findByUserId(Integer.parseInt(userId));
                if (null == user) {
                    logger.error("用户所在公司不在该大楼");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户所在公司不在该大楼"));
                }
                if (!user.getUserName().equals(userName) || user.getCompanyId() != Integer.valueOf(companyId)) {
                    logger.error("二维码数据错误，名字不匹配或者公司ID不匹配");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "二维码数据错误，名字不匹配或者公司ID不匹配"));
                } else {
                    TbDevicerelated devRelated = srv.findByFaceIP(faceRecogIp);
                    open(devRelated, user.getUserName(), user.getIdNO(), "staff");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_NORMAL, "二维码扫描成功"));
                }
            } else if (qrCodeCommonMessage.getBitMapType().equals("2")) {
                String soleCode = contentStringLists.get(1);
                if (StringUtils.isEmpty(soleCode)) {
                    logger.error("用户唯一标识码soleCode为空");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户唯一标识码soleCode为空"));
                }
                String startTime = "";
                String endTime = "";

                if (contentStringLists.size() == 4) {
                    startTime = contentStringLists.get(2);
                    endTime = contentStringLists.get(3);
                } else {
                    startTime = contentStringLists.get(9);
                    endTime = contentStringLists.get(10);
                }
                List<TbVisitor> visitorInfos = srv.findByVisitId(soleCode, startTime, endTime);
                System.out.println(visitorInfos.size());
                if (visitorInfos == null || visitorInfos.size() <= 0) {
                    logger.error("没有该visitId的访问数据");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "没有该visitId的访问数据"));
                } else {

                    TbVisitor visitorInfo = visitorInfos.get(0);
                    TbCompanyuser companyUser = srv.findByNameAndIdNO(visitorInfo.getByVisitorName(),
                            visitorInfo.getByVisitorIdCard(), "normal");
                    if (null == companyUser) {
                        logger.error("员工表无该员工数据信息");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "被访者不是该大楼员工"));
                    }
                    TbDevicerelated devRelated = srv.findByFaceIP(faceRecogIp);
                    if (devRelated.getTurnOver().equals("out")) {
                        open(devRelated, visitorInfo.getVisitorName(), visitorInfo.getVisitorIdCard(), "visitor");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_NORMAL, "二维码扫描成功"));
                    }
                    boolean success = checkUtils.verificationCache(visitorInfo);
                    if (success) {

                        TbDevice device = srv.findByDeviceIp(faceRecogIp);

                        if (null == device) {
                            logger.error("找不到该IP的设备");
                            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "找不到该IP的设备"));
                        } else {

                            String floor = companyUser.getCompanyFloor();
                            if (devRelated.getContralFloor().contains(floor)) {
                                open(devRelated, visitorInfo.getVisitorName(), visitorInfo.getVisitorIdCard(), "visitor");
                                logger.info("二维码扫描成功");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_NORMAL, "二维码扫描成功"));
                            } else {
                                logger.error("被访问者不在该楼层");
                                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "被访问者不在该楼层"));
                            }

                        }
                    } else {
                        logger.error("已过访问有效期");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "已过访问有效期"));
                    }
                }

            } else if (qrCodeCommonMessage.getBitMapType().equals("3")) {
                logger.error("二维码异常");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "失败"));
            } else {
                logger.error("二维码异常");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "失败"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("二维码异常");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "失败"));
        }
    }

    /**
     *  G区 1号楼 1楼二维码
     */
    public void scanMX861() {
        try {
            HttpServletRequest request = getRequest();
            String qrCodeIp = IPUtil.getIp(request);
            System.out.println("MX86扫描到二维码数据");
            BufferedReader reader = request.getReader();
            if (reader == null) {
                logger.error("二维码数据解析失败");
            } else {
                char[] buf = new char[1024];
                int len = 0;
                StringBuffer contentBuffer = new StringBuffer();
                while ((len = reader.read(buf)) != -1) {
                    contentBuffer.append(buf, 0, len);
                }
                // 二维码读取到的数据
                String content = contentBuffer.toString();
                // https 请求
                System.out.println("vgdecoderesult___" + content.indexOf("vgdecoderesult="));
                int vlen = "vgdecoderesult=".length();
                int i = content.indexOf("&&devicenumber");

                System.out.println(content.substring(content.indexOf("vgdecoderesult=") + vlen, i));
                content = content.substring(content.indexOf("vgdecoderesult=") + vlen, i);
                // 数组分两部分，公共标题+主内容
                String splitStrings[] = content.split("\\|");
                // 公共标题数组
                String qrCodeCommonMessage = splitStrings[0].trim();
                String[] commonMessageSplit = qrCodeCommonMessage.split("\\&");
                if(content.equals("https://u.wechat.com/MMTxGUxfjmtz6RhYSKHXmZY")){
                    renderText("code=0000");
                    logger.info("____________________________");
                    return;
                }
                if (splitStrings.length != 2 || commonMessageSplit.length != 5) {
                    //防疫二维码
                    // 二维码读取到的数据
                    TbDevicerelated tbDevicerelated = srv.findByQRCodeIP(qrCodeIp);
                    logger.info(qrCodeIp + "返回二维码数据：" + content);
                    Map<String, String> map = new HashMap<>();
                    map.put("url", content);

                    String response = null;
                    try {
                        response = okHttpUtil.post(Constants.baseURl + Constants.qrcode, map);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block

                        e.printStackTrace();
                    }
                    logger.info("服务端内容返回：" + response);
                    if (null == response) {
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "服务器错误"));
                    }
                    //查询 大楼 信息
                    TbBuildingServer buildingServer = srv.findFloor();
                    JSONObject parse = JSON.parseObject(response);
                    JSONObject verify = JSON.parseObject(parse.getString("verify"));
                    JSONObject date = JSON.parseObject(parse.getString("data"));
                    if (verify.getString("sign").equals("success")) {
                        //所有通行方式
                        if (buildingServer.getQrcodeType().equals("all")) {
                            if (tbDevicerelated.getRelayIP() != null) {
                                Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                logger.info("所有通行方式_开门成功~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "所有通行方式_开门成功~"));
                            } else {
                                logger.info("所有通行方式_开门成功~");
                                renderText("code=0000");
                            }
                            saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());

                        }
                        //禁止通行方式
                        else if (buildingServer.getQrcodeType().equals("stop")) {
                            logger.info("二维码禁止通行~");
                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                        }
                        //访客通行方式
                        else if (buildingServer.getQrcodeType().equals("visitor")) {
                            String name = (String) date.get("name");
                            String date1 = getDateT();

                            TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                            //根据姓名 和访问时间 判断是否有这个访客
                            if (tbVisitor != null) {
                                if (tbDevicerelated.getRelayIP() != null) {
                                    Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                    logger.info("访客通行方式_开门成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                                } else {
                                    logger.info("访客通行方式_开门成功~");
                                    renderText("code=0000");
                                }
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            } else {
                                TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                                if (tbCompanyuser != null) {

                                    if (tbDevicerelated.getRelayIP() != null) {
                                        Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                        logger.info("员工通行方式_开门成功~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                                    } else {
                                        logger.info("员工通行方式_开门成功~");
                                        renderText("code=0000");
                                    }
                                    saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                } else {
                                    logger.info("无权限开门~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                                }
                            }
                        } else {
                            String timeT = getTimeT();
                            //全员通行 时间段
                            if (srv.findAllDate(timeT) != null) {
                                if (tbDevicerelated.getRelayIP() != null) {
                                    Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                    logger.info("全部通行_开门成功~");
                                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "全部通行_开门成功~"));
                                } else {
                                    logger.info("全部通行_开门成功~");
                                    renderText("code=0000");
                                }
                                saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                            }
                            //访客通行 时间段
                            else if (srv.findVisitorDate(timeT) != null) {
                                String name = (String) date.get("name");
                                String date1 = getDateT();
                                TbVisitor tbVisitor = TbVisitor.dao.findFirst("select * from tb_visitor where visitorName = ? and startDateTime <= ? and endDateTime >= ?", name, date1, date1);

                                //根据姓名 和访问时间 判断是否有这个访客
                                if (tbVisitor != null) {
                                    if (tbDevicerelated.getRelayIP() != null) {
                                        Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                        logger.info("访客通行方式_开门成功~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "访客通行方式_开门成功~"));
                                    } else {
                                        logger.info("访客通行方式_开门成功~");
                                        renderText("code=0000");
                                    }
                                    saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                } else {
                                    TbCompanyuser tbCompanyuser = TbCompanyuser.dao.findFirst("select * from tb_companyuser where userName = ?", name);
                                    if (tbCompanyuser != null) {
                                        if (tbDevicerelated.getRelayIP() != null) {
                                            Control24DeviceUtil.controlDevice(tbDevicerelated.getRelayIP(), Integer.parseInt(tbDevicerelated.getRelayPort()), tbDevicerelated.getRelayOUT(), "");
                                            logger.info("员工通行方式_开门成功~");
                                            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "员工通行方式_开门成功~"));
                                        } else {
                                            logger.info("员工通行方式_开门成功~");
                                            renderText("code=0000");
                                        }
                                        saverecord(String.valueOf(date.get("name")), String.valueOf(date.get("phone")), String.valueOf(date.get("checkStatus")), tbDevicerelated.getQRCodeIP(), tbDevicerelated.getTurnOver());
                                    } else {
                                        logger.info("无权限开门~");
                                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "无权限开门~"));
                                    }
                                }

                            }
                            //禁止通行 时间段
                            else if (srv.findStopDate(timeT) != null) {
                                logger.info("二维码禁止通行~");
                                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "二维码禁止通行~"));
                            }
                        }

                    } else {
                        logger.error("开门失败~");
                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "开门失败~"));
                    }

                } else {
                    System.out.println("二维码类型:" + commonMessageSplit[1]);
                    if (commonMessageSplit[1].equals("4") || commonMessageSplit[1].equals("5")) {
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);
                        int recirdId = Integer.valueOf(contentStringLists.get(1));

                        TbShareroom shareroom = srv.findByRecordId(recirdId);
                        if (null == shareroom) {
                            logger.error("该二维码数据不存在");
                            return;
                        }
                        if (!shareroom.getApplyDate().equals(contentStringLists.get(2))
                                || !shareroom.getApplyStartTime().equals(contentStringLists.get(3))
                                || !shareroom.getApplyEndTime().equals(contentStringLists.get(4))) {
                            logger.error("二维码时间数据与本地时间数据不一致");
                            renderNull();
                            return;
                        }

                        if (shareroom.getRecordStatus() == 1 || shareroom.getRecordStatus() == 4) {
                            logger.error("订单状态无效");
                            renderNull();
                            return;
                        }

                        TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);

                        if (null == devRelated) {
                            logger.error("数据库中未找到二维码读头IP地址");
                            renderNull();
                            return;
                        }
                        if (devRelated.getTurnOver().equals("out")) {
                            open(devRelated, shareroom.getUserName(), shareroom.getIdNo(), "applicant");
                        }
                        if (!devRelated.getContralFloor().contains(shareroom.getRoomAddr())) {
                            logger.error("二维码访问楼层错误");
                            renderNull();
                            return;
                        }
                        // 判断时间是否在有效期
                        // System.out.println(Misc.compareDate2(shareroom.getApply_date(), getDate()));
                        if (Misc.compareDate2(shareroom.getApplyDate(), getDate())) {
                            int startSecond = (int) (Float.parseFloat(shareroom.getApplyStartTime()) * 3600);
                            int endSecond = (int) (Float.parseFloat(shareroom.getApplyEndTime()) * 3600);

                            if (getSecond() > startSecond && getSecond() < endSecond) {
                                String key = shareroom.getUserName() + "_" + shareroom.getIdNo();
                                if (null == redisUtils.get(key)) {
                                    open(devRelated, shareroom.getUserName(), shareroom.getIdNo(), "applicant");
                                    redisUtils.set(key, "locaked");
                                    redisUtils.expire(key, 2);
                                }

                            } else {
                                logger.error("已过访问有效期");
                                renderNull();
                                return;
                            }
                        } else {
                            logger.error("已过访问有效期");
                            renderNull();
                            return;
                        }
                    } else if (commonMessageSplit[1].equals("1")) {
                        //门禁二维码
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);

                        String userName = contentStringLists.get(0);
                        String companyId = contentStringLists.get(1);
                        userId = contentStringLists.get(2);

                        TbCompanyuser user = srv.findByUserId(Integer.valueOf(userId));
                        if (null == user) {
                            logger.error("用户所在公司不在该大楼");
                            renderNull();
                            return;
                        }
                        if (!user.getUserName().equals(userName)) {
                            logger.error("二维码数据错误，名字不匹配");
                            renderNull();
                        } else {
                            TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);
                            open(devRelated, user.getUserName(), user.getIdNO(), "staff");
                            renderText("code=0000");
                        }
                    } else if (commonMessageSplit[1].equals("2")) {
                        //门禁二维码
                        String strByte = new String(Base64_2.decode(splitStrings[1].trim()), "UTF-8");
                        logger.info("二维码解密主内容：" + strByte);
                        // 二维码主内容数据
                        List<String> contentStringLists = parseContent(strByte);

                        String userName = contentStringLists.get(0);
                        userId = contentStringLists.get(1);
                        startDateTime = contentStringLists.get(2);
                        endDateTime = contentStringLists.get(3);

                        TbVisitor visitor = srv.findByVisitorId(Integer.valueOf(userId));
                        if (null == visitor) {
                            logger.error("用户所在公司不在该大楼");
                            renderNull();
                            return;
                        }
                        if (!visitor.getVisitorName().equals(userName)) {
                            logger.error("二维码数据错误，名字不匹配");
                            renderNull();
                        } else {
                            TbDevicerelated devRelated = srv.findByQRCodeIP(qrCodeIp);
                            TbVisitor tbVisitor = srv.findByVisitDate(startDateTime, endDateTime);
                            if (tbVisitor == null) {
                                logger.info("访客不在在有效期, 禁止开门");
                                renderNull();
                            } else {
                                open(devRelated, visitor.getVisitorName(), visitor.getVisitorIdCard(), "staff");
                                renderText("code=0000");
                            }


                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("二维码异常");

        }
    }
}
