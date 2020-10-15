package com.xiaosong.common.network;

import com.jfinal.core.Controller;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbNetwork;
import com.xiaosong.util.RetUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 网络配置
 */

public class NetWorkController extends Controller {
    private static Logger logger = Logger.getLogger(NetWorkController.class);
    private static NetWorkService srv = NetWorkService.me;

//    // local - 接口名称
//    // static - 设置使用本地静态配置设置IP地址。
//    // 10.0.0.9 - 要修改的ip
//    // 255.0.0.0 － 子网掩码
//    // 10.0.0.1 － 网关，如果为none: 不设置默认网关。
//    // 1 －默认网关的跃点数。如果网关设置为 ’none’，则不应设置此字段。
//    public void index() throws Exception {
//        try {
//            String ip = getPara("ip");
//            String getWay = getPara("getWay");
//            String mask = getPara("mask");
//            String wifi = getPara("wifi");
//            String wifiIp = getPara("wifiIp");
//            String wifiGetWay = getPara("wifiGetWay");
//            String wifiMask = getPara("wifiMask");
//
//            String wifi1 = "";
//            TbNetwork tbNetwork = getModel(TbNetwork.class);
//            if (wifi.equals("0")) {
//                //开启wifi 操作
//                wifi1 = "true";
//                tbNetwork.setLocalIp(ip);
//                tbNetwork.setLocalGetWay(getWay);
//                tbNetwork.setLocalMask(mask);
//                tbNetwork.setWifi(wifi1);
//                tbNetwork.setWifiIp(wifiIp);
//                tbNetwork.setWifiGetWay(wifiGetWay);
//                tbNetwork.setWifiMask(wifiMask);
//                boolean save = tbNetwork.save();
//                if (save) {
//                    //修改 上位机 IP地址 网关
//                    Runtime.getRuntime().exec("netsh    interface    ip    set    addr    \"本地连接\"    static    "
//                            + ip + "    " + mask + "     " + getWay + "     1");
//                    //开启wifi
//                    Runtime.getRuntime().exec(" /sbin/ifconfig wlan0 up ");
//                    //修改wifi ip
//                    Runtime.getRuntime().exec(" ifconfig eth0 " + wifiIp + " netmask " + wifiMask);
//                    //修改wifi 网关
//                    Runtime.getRuntime().exec(" route add default gw " + wifiGetWay);
//                    logger.info("命令已执行~");
//                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "命令已执行~"));
//                } else {
//                    logger.error("数据保存错误~");
//                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "数据保存错误~"));
//                }
//            } else {
//                //关闭wifi  操作
//                wifi1 = "false";
//                tbNetwork.setLocalIp(ip);
//                tbNetwork.setLocalGetWay(getWay);
//                tbNetwork.setLocalMask(mask);
//                tbNetwork.setWifi(wifi1);
//                tbNetwork.save();
//                boolean save = tbNetwork.save();
//                if (save) {
//                    Runtime.getRuntime().exec("netsh    interface    ip    set    addr    \"本地连接\"    static    "
//                            + ip + "    " + mask + "     " + getWay + "     1");
//                    //关闭wifi
//                    Runtime.getRuntime().exec(" /sbin/ifconfig wlan0 down ");
//                    logger.info("命令已执行~");
//                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "命令已执行~"));
//                } else {
//                    logger.error("数据保存错误~");
//                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "数据保存错误~"));
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            logger.info("命令执行异常~");
//            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "命令执行异常~"));
//        }
//    }

    public void update() {
        try {
            String ip = getPara("ip");
            String getWay = getPara("getWay");
            String mask = getPara("mask");
            String wifi = getPara("wifi");
            String wifiIp = getPara("wifiIp");
            String wifiGetWay = getPara("wifiGetWay");
            String wifiMask = getPara("wifiMask");
            TbNetwork netWork = srv.findNetWork();
            if (ip == null || getWay == null || mask == null) {
                logger.error("参数不能为null");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "参数不能为null~"));
            }else{
                if (netWork != null) {
                    linuxShellexec("/usr/tomcat/wincc/updateIp.sh "
                            + netWork.getLocalIp() + " " + ip + " "
                            + netWork.getLocalGetWay() + " " + getWay + " "
                            + netWork.getLocalMask() + " " + mask + " "
                    );
                    netWork.setLocalIp(ip);
                    netWork.setLocalGetWay(getWay);
                    netWork.setLocalMask(mask);
                    //开启wifi
                    if (wifi != null) {
                        if (wifi.equals("true")) {
                            linuxShellexec("/usr/tomcat/wincc/updateWifi.sh "
                                    + netWork.getWifiIp() + " " + wifiIp + " "
                                    + netWork.getWifiGetWay() + " " + wifiGetWay + " "
                                    + netWork.getWifiMask() + " " + wifiMask + " " + "true"
                            );
                            netWork.setWifiIp(wifiIp);
                            netWork.setWifiGetWay(wifiGetWay);
                            netWork.setWifiMask(wifiMask);
                        } else {
                            linuxShellexec("/usr/tomcat/wincc/updateWifi.sh "
                                    + "192.169.255.255" + " " + "192.169.255.255" + " "
                                    + "192.169.255.255" + " " + "192.169.255.255" + " "
                                    + "192.169.255.255" + " " + "192.169.255.255" + " " + "false"
                                   //+ netWork.getWifiIp() + " " + wifiIp + " "
                                   //+ netWork.getWifiGetWay() + " " + wifiGetWay + " "
                                   //+ netWork.getWifiMask() + " " + wifiMask + " " + "false"
                            );
                        }
                        netWork.setWifi(wifi);
                    }
                    netWork.update();
                    //重启程序
                    //linuxShellexec("/usr/tomcat/wincc/jfinal.sh stop ");
                    //Thread.sleep(4000);
                    //linuxShellexec("/usr/tomcat/wincc/jfinal.sh start ");
                    logger.info("上位机IP已修改~");
                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "上位机IP已修改~"));
                } else {
                    logger.error("操作暂时无法执行~");
                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "操作暂时无法执行~"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("命令执行异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "命令执行异常~"));
        }

    }

    public void index() {
        try {
            TbNetwork netWork = srv.findNetWork();
            if (netWork != null) {
                logger.info("查询成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, netWork));
            } else {
                logger.error("查询失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "查询失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "查询异常~"));
        }


    }

    /**
     * 执行脚本并传入参数
     *
     * @param shellPath
     * @return
     */
    public String linuxShellexec(String shellPath) {
        String result = "";
        try {
            Process ps = Runtime.getRuntime().exec(shellPath);
            ps.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            result = "linux下运行完毕";
        }
        return result;

    }
}