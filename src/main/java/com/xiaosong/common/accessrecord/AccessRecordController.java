package com.xiaosong.common.accessrecord;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.constant.Constants;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbAccessrecord;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.util.MD5Util;
import com.xiaosong.util.OkHttpUtil;
import com.xiaosong.util.RetUtil;
import com.xiaosong.util.XLSFileKit;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 通行人员
 */
public class AccessRecordController extends Controller {
    private static AccessRecordService srv = AccessRecordService.me;
    private static Logger logger = Logger.getLogger(AccessRecordController.class);

    /**
     * 报表下载
     */
    public void dow() {
        try {
            String userName = getPara("userName");        //用户姓名
            String userType = getPara("userType");            //员工类型
            String turnOver = getPara("turnOver");          //进出标识
            String deviceIp = getPara("deviceIp");        //设备ip
            String beginDate = getPara("beginDate");       //开始时间
            String endDate = getPara("endDate");          //结束时间

            // 导出`Excel`名称
            String fileName = "通行人员报表_" + getDate() + ".xls";

            // excel`保存路径
            String filePath = getRequest().getRealPath("/") + "/file/export/";
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
            filePath += fileName;
            XLSFileKit xlsFileKit = new XLSFileKit(filePath);
            List<List<Object>> content = new ArrayList<List<Object>>();
            List<String> title = new ArrayList<String>();
            // 添加`title`,对应的从数据库检索出`datas`的`title`
            title.add("序号");
            title.add("通行日期");
            title.add("通行时间");
            title.add("姓名");
            title.add("人员类型");
            title.add("设备类型");
            title.add("设备IP");
            title.add("进出类型");
            //根据组名查询人员信息
            List<TbAccessrecord> tbAccessrecordList = srv.findByBim(userName, userType, turnOver, deviceIp, beginDate, endDate);
            int i = 0;
            OK:
            while (true) {
                if (tbAccessrecordList.size() < (i + 1)) {
                    break OK;
                }
                // 判断单元格是否为空，不为空添加数据
                int index = i + 1;
                List<Object> row = new ArrayList<Object>();
                row.add(index + "");
                //row.add(null == tbStatements.get(i).getId() ? "" : tbStatements.get(i).getId());
                row.add(null == tbAccessrecordList.get(i).get("scanDate") ? "" : tbAccessrecordList.get(i).get("scanDate"));
                row.add(null == tbAccessrecordList.get(i).get("scanTime") ? "" : tbAccessrecordList.get(i).get("scanTime"));
                row.add(null == tbAccessrecordList.get(i).get("userName") ? "" : tbAccessrecordList.get(i).get("userName"));
                row.add("staff".equals(tbAccessrecordList.get(i).get("userType")) ? "员工" : "访客");
                row.add("FACE".equals(tbAccessrecordList.get(i).get("deviceType")) ? "人脸设备" : "二维码设备");
                row.add(null == tbAccessrecordList.get(i).get("deviceIp") ? "" : tbAccessrecordList.get(i).get("deviceIp"));
                row.add("in".equals(tbAccessrecordList.get(i).get("turnOver")) ? "进" : "出");
                content.add(row);
                i++;
            }

            xlsFileKit.addSheet(content, "通行人员报表", title);
            boolean save = xlsFileKit.save();
            if (save) {
                logger.info("报表导出成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "报表导出成功~"));
                File file1 = new File(getRequest().getRealPath("/") + "/file/export/" + "通行人员报表_" + getDate() + ".xls");
                renderFile(file1);
            } else {
                logger.error("报表导出失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "报表导出失败~"));
            }
//        renderJson(new Record().set("relativePath", relativePath));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("报表导出异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "报表导出异常~"));
        }
    }

    /**
     * 条件查询  通行记录
     */
    public void index() {
        try {
            String userName = getPara("userName");        //用户姓名
            String userType = getPara("userType");            //员工类型
            String turnOver = getPara("turnOver");          //进出标识
            String deviceIp = getPara("deviceIp");        //设备ip
            String beginDate = getPara("beginDate");       //开始时间
            String endDate = getPara("endDate");          //结束时间
            List<TbAccessrecord> list = new ArrayList<>();
            int page = Integer.parseInt(getPara("currentPage"));  //当前页
            int number = Integer.parseInt(getPara("pageSize"));   //一页显示数量
            int index = (page - 1) * number;
            //条件为空查询所有
            if (userName == null && userType == null && turnOver == null && deviceIp == null && beginDate == null && endDate == null) {
                Page<Record> all = srv.findAll(page, number);
                if (all != null) {
                    logger.info("通行人员查询成功~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, all, all.getPageSize()));
                } else {
                    logger.error("通行人员查询失败~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "通行人员查询失败"));
                }
            }
            List<TbAccessrecord> tbStatementList = srv.findByBim(userName, userType, turnOver, deviceIp, beginDate, endDate);
            for (int i = index; i < tbStatementList.size() && i < (index + number); i++) {
                list.add(tbStatementList.get(i));
            }

            if (tbStatementList!=null) {
                logger.info("通行数据分页查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, tbStatementList.size()));
            } else {
                logger.error("通行数据分页查询失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "通行数据分页查询失败"));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            logger.error("通行数据分页查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "通行数据分页查询异常"));
        }
    }

    /**
     * 获取当前系统时间 年-月-日
     *
     * @return
     */
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

    /**
     * 发送 通行记录
     */
    public void send() {

        try {
            logger.info("搜索堆积的扫描通过数据记录------------------");
            OkHttpUtil okHttpUtil = new OkHttpUtil();

            List<TbAccessrecord> accessRecordList = DeviceService.me.findByIsSendFlag("F");
            if (accessRecordList.size() <= 0) {
                System.out.println("无堆积的扫描结果数据，已全部发送");
                logger.info("无堆积的扫描结果数据，已全部发送");
                return;
            }
            File filepath = new File(Constants.AccessRecPath);
            if (!filepath.exists()) {
                filepath.mkdirs();
            }
            File file = new File(Constants.AccessRecPath, "access1.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            Integer startId = accessRecordList.get(0).getId();
            Integer endId = accessRecordList.get(accessRecordList.size() - 1).getId();
            for (int i = 0; i < accessRecordList.size(); i++) {

                StringBuilder context = new StringBuilder();
                context.append(accessRecordList.get(i).getOrgCode());
                context.append("|" + accessRecordList.get(i).getPospCode());
                context.append("|" + accessRecordList.get(i).getScanDate());
                context.append("|" + accessRecordList.get(i).getScanTime());
                context.append("|" + accessRecordList.get(i).getTurnOver());
                context.append("|" + "null");
                context.append("|" + accessRecordList.get(i).getDeviceType());
                context.append("|" + accessRecordList.get(i).getDeviceIp());
                context.append("|" + accessRecordList.get(i).getUserType());
                context.append("|" + accessRecordList.get(i).getUserName());
                context.append("|" + accessRecordList.get(i).getIdCard());
                writeInFile(file, context.toString()); // 写入文件
            }
            TbBuildingServer serverSer = ServerService.me.findSer();
            Map<String, Object> map = new HashMap<>();
            map.put("pospCode", serverSer.getPospCode());

            map.put("orgCode", serverSer.getOrgCode());
            String keyStr = serverSer.getOrgCode() + serverSer.getPospCode() + serverSer.getKey();
            String sign = MD5Util.MD5(keyStr);
            map.put("sign", sign);
            map.put("file", file);

            StringBuilder stringBuilder = new StringBuilder();
            //stringBuilder.append(Constants.baseFileURl);

            stringBuilder.append("http://" + serverSer.getServer2Ip() + ":" + serverSer.getServer2Port());

            stringBuilder.append(Constants.accessRecordByBatch);
            //stringBuilder.append(Constants.ceshi2);
            String url = stringBuilder.toString();
            String sendResponse = okHttpUtil.postFile(url, map, "multipart/form-data");
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write("");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logger.info(sendResponse);
            if (sendResponse.isEmpty() || sendResponse == null) {
                logger.error("发送通行记录失败");
                //logger.sendErrorLog(towerInforService.findOrgId(), "发送通行记录失败", "","网络错误", Constants.errorLogUrl,keyStr);
                return;
            }

            JSONObject jsonObject = JSONObject.parseObject(sendResponse);
            Map<String, Object> verifyReceive = (Map<String, Object>) jsonObject;
            JSONObject verify = (JSONObject) verifyReceive.get("verify");
            if (verify.get("sign").equals("success")) {
                DeviceService.me.updateSendFlag(startId, endId);
                logger.info("通行记录发送成功");
            }
            renderNull();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void writeInFile(File file, String content) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
            StringBuilder outputString = new StringBuilder();
            outputString.append(content + "\r\n");
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

    }

//    /**
//     *  查询 通行人数
//     */
//    public void find(){
//        try {
//            String date = getDate();
//            //人脸设备
//            Integer face = srv.findFace(date);
//            //二维码设备
//            Integer qrCode = srv.findQRCode(date);
//            //员工通行
//            Integer staff = srv.findStaff(date);
//            //访客通行
//            Integer visitor = srv.findVisitor(date);
//            Map<String,Integer> map = new HashMap<>();
//            map.put("face",face);
//            map.put("qrCode",qrCode);
//            map.put("staff",staff);
//            map.put("visitor",visitor);
//
//            logger.info("人脸设备通行人数:"+face+" ,二维码设备通行人数:"+qrCode+" ,员工通行人数:"+staff+" ,访客通行人数:"+visitor);
//            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, map,map.size()));
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("查询异常~");
//            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR,"查询异常"));
//        }
//    }

    public void findDate(){
        try {
            String yz_time=getTimeInterval(new Date());//获取本周时间
            String array[]=yz_time.split(",");
            String start_time=array[0];//本周第一天
            String end_time=array[1];  //本周最后一天
            //格式化日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date dBegin = sdf.parse(start_time);
            Date dEnd = sdf.parse(end_time);
            List<Date> lDate = findDates(dBegin, dEnd);//获取这周所有date
            String zy = sdf.format(lDate.get(0)); //周一
            String ze = sdf.format(lDate.get(1)); //周二
            String zs = sdf.format(lDate.get(2)); //周三
            String zS = sdf.format(lDate.get(3)); //周四
            String zw = sdf.format(lDate.get(4)); //周五
            String zl = sdf.format(lDate.get(5)); //周六
            String zq = sdf.format(lDate.get(6)); //周日

            List<Object> listStaff = new ArrayList<>();
            List<Object> listVisitor = new ArrayList<>();
            String[] str = {zy,ze,zs,zS,zw,zl,zq};

            for (String s : str) {
                Integer date = srv.findStaff(s);
                listStaff.add(date);
            }
            System.out.println(listStaff.get(1));
            for (String s : str) {
                Integer date = srv.findVisitor(s);
                listVisitor.add(date);
            }
            Map<String,Object> map = new HashMap<>();
            map.put("staff",listStaff);
            map.put("visitor",listVisitor);

            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, map));
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    public static String getTimeInterval(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 判断要计算的日期是否是周日，如果是则减一天计算周六的，否则会出问题，计算到下一周去了
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);// 获得当前日期是一个星期的第几天
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        // System.out.println("要计算日期为:" + sdf.format(cal.getTime())); // 输出要计算日期
        // 设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        // 获得当前日期是一个星期的第几天
        int day = cal.get(Calendar.DAY_OF_WEEK);
        // 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        String imptimeBegin = sdf.format(cal.getTime());
        // System.out.println("所在周星期一的日期：" + imptimeBegin);
        cal.add(Calendar.DATE, 6);
        String imptimeEnd = sdf.format(cal.getTime());
        // System.out.println("所在周星期日的日期：" + imptimeEnd);
        return imptimeBegin + "," + imptimeEnd;
    }
    public static List<Date> findDates(Date dBegin, Date dEnd)
    {
        List lDate = new ArrayList();
        lDate.add(dBegin);
        Calendar calBegin = Calendar.getInstance();
        // 使用给定的 Date 设置此 Calendar 的时间
        calBegin.setTime(dBegin);
        Calendar calEnd = Calendar.getInstance();
        // 使用给定的 Date 设置此 Calendar 的时间
        calEnd.setTime(dEnd);
        // 测试此日期是否在指定日期之后
        while (dEnd.after(calBegin.getTime()))
        {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            calBegin.add(Calendar.DAY_OF_MONTH, 1);
            lDate.add(calBegin.getTime());
        }
        return lDate;
    }


}
