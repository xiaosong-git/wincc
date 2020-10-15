package com.xiaosong.config.quartz;

import com.alibaba.fastjson.JSONObject;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.constant.Constants;
import com.xiaosong.model.TbAccessrecord;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.util.MD5Util;
import com.xiaosong.util.OkHttpUtil;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时发送通行记录
 */
//public class AccessRecordToYum implements Job {
public class AccessRecordToYum implements Job {
    private DeviceService srvDevice = DeviceService.me; //设备服务
    private ServerService srvServer = ServerService.me; //服务器业务层

    private static Logger logger = Logger.getLogger(AccessRecordToYum.class); //log日志

    /**
     * 下发记录
     * @throws Exception
     */
    public void sendAccessRecord() throws Exception {
        //srvDevice.deleteSurplus();
        logger.info("搜索堆积的扫描通过数据记录------------------");
        OkHttpUtil okHttpUtil = new OkHttpUtil();

        List<TbAccessrecord> accessRecordList = srvDevice.findByIsSendFlag("F");
        if(accessRecordList.size() <= 0) {
            System.out.println("无堆积的扫描结果数据，已全部发送");
            logger.info("无堆积的扫描结果数据，已全部发送");
            return;
        }
        File filepath = new File(Constants.AccessRecPath);
        if(!filepath.exists()) {
            filepath.mkdirs();
        }
        File file = new File(Constants.AccessRecPath,"access1.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Integer startId = accessRecordList.get(0).getId();
        Integer endId= accessRecordList.get(accessRecordList.size()-1).getId();
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
        TbBuildingServer serverSer = srvServer.findSer();
        Map<String,Object> map = new HashMap<>();
        map.put("pospCode", serverSer.getPospCode());

        map.put("orgCode", serverSer.getOrgCode());
        String keyStr = serverSer.getOrgCode()+serverSer.getPospCode()+serverSer.getKey();
        String sign = MD5Util.MD5(keyStr);
        map.put("sign", sign);
        map.put("file", file);

        StringBuilder stringBuilder = new StringBuilder();
        //stringBuilder.append(Constants.baseFileURl);

        stringBuilder.append("http://"+serverSer.getServer2Ip()+":"+serverSer.getServer2Port());

        stringBuilder.append(Constants.accessRecordByBatch);
        //stringBuilder.append(Constants.ceshi2);
        String url = stringBuilder.toString();
        String sendResponse =okHttpUtil.postFile(url, map, "multipart/form-data");
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
        if(sendResponse.isEmpty() || sendResponse == null) {
            logger.error("发送通行记录失败");
            //logger.sendErrorLog(towerInforService.findOrgId(), "发送通行记录失败", "","网络错误", Constants.errorLogUrl,keyStr);
            return;
        }

        JSONObject  jsonObject = JSONObject.parseObject(sendResponse);
        Map<String,Object> verifyReceive = (Map<String,Object>)jsonObject;
        JSONObject  verify = (JSONObject) verifyReceive.get("verify");
        if(verify.get("sign").equals("success")) {
            srvDevice.updateSendFlag(startId, endId);
            logger.info("通行记录发送成功");
        }

    }
    private void writeInFile(File file, String content) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(file, true),"UTF-8");
            StringBuilder outputString = new StringBuilder();
            outputString.append(content + "\r\n");
            out.write(outputString.toString());

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try {
                out.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }

    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            sendAccessRecord();
            srvDevice.UpdateDownNum();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
