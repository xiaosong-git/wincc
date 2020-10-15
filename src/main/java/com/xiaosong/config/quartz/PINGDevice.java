package com.xiaosong.config.quartz;

import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Db;
import com.xiaosong.common.device.DeviceService;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.websocket.WebSocket;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbPtinfo;
import okhttp3.*;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时运行PING命令，查看上位机与人脸设备的连通性
 */
public class PINGDevice implements Job {

    private ServerService srvFloor = ServerService.me;
    private DeviceService srvDevice = DeviceService.me;
    private static Logger logger = Logger.getLogger(DelGoneVisitorRec.class);
    WebSocket webSocketClient = new WebSocket();
    //    private WebSocketClientUtil webSocketClient = new WebSocketClientUtil() ;
    private String osName = System.getProperty("os.name");
    //网络速度
    private static double avg = 0;

    private Prop use = PropKit.use("config_product.properties");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // TODO Auto-generated method stub
        TbBuildingServer buildingServer = srvFloor.findNetType();
        if ("1".equals(buildingServer.getNetType()) || "2".equals(buildingServer.getNetType())) {
            try {
//                if(webSocketClient.isClosed()) {
//                    webSocketClient.reconnect();
//                }
                ping();
//                sendDeviceStatus();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            logger.info("网络设置内网，无法使用Ping功能");
        }
    }

    /**
     * 发送设备状态
     */
    private void sendDeviceStatus() {
        String url = use.get("deviceStatusUrl");

        List<TbPtinfo> tbPtinfos = TbPtinfo.dao.find("select * from tb_ptinfo");
        for (TbPtinfo tbPtinfo : tbPtinfos) {
            try {
                OkHttpClient client = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                RequestBody body = RequestBody.create(mediaType,
                        "ip="+tbPtinfo.getDeviceIP()+
                                "&deviceName="+tbPtinfo.getExpt1()+
                                "&type="+tbPtinfo.getDeviceName()+
                                "&gate="+tbPtinfo.getOrgCode()+
                                "&status="+tbPtinfo.getPingStatus()+
                                "&avg="+tbPtinfo.getPingavg()
                );
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();

                Response response = client.newCall(request).execute();
                logger.info("发送设备状态："+response.code());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *  ping 设备状态
     * @throws IOException
     */
    private void ping() throws IOException {
        logger.info("当前系统为：" + osName);

        List<TbDevice> devices = srvDevice.findDevice();
        for (TbDevice faceDec : devices) {
            if (faceDec.getDeviceIp().equals("") || faceDec.getDeviceIp() == null) {
                logger.error(faceDec.getDeviceId() + "设备IP地址为空");
                continue;
            }
            boolean bool = pingDev(faceDec.getDeviceIp(), 4, 1000);

            String status = "";
            if(bool){
                status="normal";
            }else{
                status="error";
            }

            TbPtinfo first = TbPtinfo.dao.findFirst("select * from tb_ptinfo where deviceIP = ?", faceDec.getDeviceIp());

            String orgCode = ServerService.me.findByOrgCode();
            if (first==null) {
                TbPtinfo tbPtinfo = new TbPtinfo();
                tbPtinfo.setDeviceName(faceDec.getDeviceMode());
                tbPtinfo.setDeviceIP(faceDec.getDeviceIp());
                tbPtinfo.setOrgCode(orgCode);
                tbPtinfo.setPingStatus(status);
                tbPtinfo.setPingavg(avg);
                tbPtinfo.setExpt1(faceDec.getDeviceType());
                tbPtinfo.setFreshTime(getDateTime());
                tbPtinfo.save();
            } else {
                System.out.println("UPDATE tb_ptinfo SET deviceName='"+faceDec.getDeviceMode()+"', deviceIP='"+faceDec.getDeviceIp()+
                        "', orgCode='"+orgCode+"', pingStatus='"+status +
                        "', expt1='null', expt2='null', freshTime='"+getDateTime()+"'"+
                        "WHERE deviceIP='"+faceDec.getDeviceIp()+"'");
                Db.update("UPDATE tb_ptinfo SET deviceName='"+faceDec.getDeviceMode()+"', deviceIP='"+faceDec.getDeviceIp()+
                        "', orgCode='"+orgCode+"', pingStatus='"+status +
                        "', expt1='"+faceDec.getDeviceType()+"', expt2='null', freshTime='"+getDateTime()+"'"+
                        " WHERE deviceIP='"+faceDec.getDeviceIp()+"'");
            }
        }

    }

    /**
     * @param ipAddress  ip地址
     * @param pingTimes  次数(一次ping,对方返回的ping的结果的次数)
     * @param timeOut    超时时间 单位ms(ping不通,设置的此次ping结束时间)
     * @return
     */
    public static boolean pingDev(String ipAddress, int pingTimes, int timeOut) {
        BufferedReader in = null;
        String pingCommand = null;
        Runtime r = Runtime.getRuntime();
        String osName = System.getProperty("os.name");
        if(osName.contains("Windows")){
            //将要执行的ping命令,此命令是windows格式的命令
            pingCommand = "ping " + ipAddress + " -n " + pingTimes    + " -w " + timeOut;
        }else{
            //将要执行的ping命令,此命令是Linux格式的命令
            //-c:次数,-w:超时时间(单位/ms)  ping -c 10 -w 0.5 192.168.120.206
            pingCommand = "ping " + " -c " + "4" + " -w " + "2 " + ipAddress;
        }
        try {
            //执行命令并获取输出
            Process p = r.exec(pingCommand);
            if (p == null) {
                return false;
            }
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int connectedCount = 0;
            String line = null;
            while ((line = in.readLine()) != null) {
//                connectedCount += getCheckResult(line,osName);
                connectedCount += getCheckResult(line,osName);
                if(line.contains("ms")){
                    if(line.length()<42){
                        break;
                    }
                    String[] ms = line.split("ms");
                    String m = ms[0];
                    String substring = m.substring(m.length() - 4, m.length());
                    String regEx = "[^0-9]";
                    Pattern p1 = Pattern.compile(regEx);
                    Matcher m1 = p1.matcher(substring);
                    double userId = Integer.parseInt(m1.replaceAll("").trim());
                    avg+=userId;
                }
            }
            avg=avg/4;
            //如果出现类似=23 ms ttl=64(TTL=64 Windows)这样的字样,出现的次数=测试次数则返回真
            //return connectedCount == pingTimes;
            return connectedCount >= 2 ? true : false;
        } catch (Exception ex) {
            ex.printStackTrace(); //出现异常则返回假
            return false;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //若line含有=18 ms ttl=64字样,说明已经ping通,返回1,否則返回0.
    private static int getCheckResult(String line,String osName) {
        if(osName.contains("Windows")){
            if(line.contains("TTL=")){
                return 1;
            }
        }else{
            if(line.contains("ttl=")){
                return 1;
            }
        }
        return 0;
    }


    //获取当前时间的  年-月-日  时:分:秒
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }

}
