package com.xiaosong;


import cn.dreampie.quartz.QuartzPlugin;
import com.jfinal.config.*;
import com.jfinal.ext.handler.UrlSkipHandler;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.log.Log4jLogFactory;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;

import com.jfinal.plugin.redis.RedisPlugin;
import com.jfinal.server.undertow.UndertowServer;
import com.jfinal.template.Engine;
import com.xiaosong.netty.PMSWebSocketClient;
import com.xiaosong.config.InitDevice;
import com.xiaosong.constant.Constant;
import com.xiaosong.interceptor.AuthInterceptor;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model._MappingKit;
import com.xiaosong.routes.GlobalRoutes;
import com.xiaosong.sdkConfig.HCNetSDKService;
import org.apache.log4j.Logger;

import java.util.List;

public class MainConfig extends JFinalConfig {

    private static Logger logger = Logger.getLogger(MainConfig.class);
    private static Prop p = loadConfig();
    HCNetSDKService hcNetSDKService =HCNetSDKService.me;

    private static Prop loadConfig() {
        //PropKit 工具类用来操作外部配置文件。
        //PropKit 可以极度方便地在系统任意时空使用
        //生产模式时加载product文件，开发时加载develop文件，避免生产打包时文件漏改，只需改Constant中的dev_mode
        if(Constant.DEV_MODE){
            return PropKit.use("db_develop.properties").append("config_develop.properties");
        }else{
            return PropKit.use("db_product.properties").append("config_product.properties");
        }

    }

    @Override
    public void configConstant(Constants me) {
        me.setDevMode(Constant.DEV_MODE);//是否开发模式 上生产是需要改变 与JFInal框架有关
        me.setMaxPostSize(1024 * 1024 * 20);//默认最大上传数据大小
        me.setLogFactory(new Log4jLogFactory());//日志配置
        System.out.println("当前系统为：" + JudgeSystem() + "系统~");
        System.out.println("当前路径:"+PathKit.getWebRootPath());
//        if (isWindows()) {
        me.setBaseUploadPath(com.xiaosong.constant.Constants.BASE_UPLOAD_PATH);//文件上传路径
        me.setBaseDownloadPath(com.xiaosong.constant.Constants.BASE_DOWNLOAD_PATH);//文件下载路径
//        } else {
//            me.setBaseUploadPath(com.xiaosong.constant.Constants.BASE_UPLOAD_PATH1);//文件上传路径
//            me.setBaseDownloadPath(com.xiaosong.constant.Constants.BASE_DOWNLOAD_PATH1);//文件下载路径
//        }
    }

    @Override
    public void configRoute(Routes routes) {
        routes.add(new GlobalRoutes());
    }

    @Override
    public void configEngine(Engine me) {
        me.setDevMode(Constant.DEV_MODE);
        me.addSharedObject("sk", new com.jfinal.kit.StrKit());
    }

    @Override
    public void configPlugin(Plugins plugins) {
        //数据库插件
        DruidPlugin druidPlugin = new DruidPlugin(PropKit.get("jdbcUrl"),PropKit.get("user"),PropKit.get("password"),PropKit.get("driver"));
        ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
        arp.setShowSql(true);
        arp.setDialect(new MysqlDialect());
        _MappingKit.mapping(arp);
        plugins.add(druidPlugin);
        plugins.add(arp);
        //定时任务插件
        QuartzPlugin quartzPlugin = new QuartzPlugin();
        quartzPlugin.setJobs("job.properties");
        plugins.add(quartzPlugin);

        //Redis 以及缓存Cache结合使用配置
        //后面使用Redis获取数据，接口CacheAPI进行各种操作
        String cacheType = PropKit.get("cache.type").trim();
        if("redis".equals(cacheType)){
            RedisPlugin redis=new RedisPlugin("xiaosong", "127.0.0.1");
            plugins.add(redis);
        }
    }

    @Override
    public void configInterceptor(Interceptors interceptors) {
        interceptors.addGlobalActionInterceptor(new AuthInterceptor());
    }

    @Override
    public void configHandler(Handlers handlers) {
        handlers.add(new UrlSkipHandler("^/websocket.ws", false));
    }

    //判断是否为 winds系统
    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    //判断是否为linux系统
    public boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }
    //查看当前系统
    public String JudgeSystem() {
        if (isLinux()) {
            return "linux";
        } else if (isWindows()) {
            return "windows";
        } else {
            return "other system";
        }
    }
    @Override
    public void afterJFinalStart() {
        PMSWebSocketClient pms = new PMSWebSocketClient();
        try {
            //和设备长连接
            //new StringTcpServer(10008).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        pms.login();
        //和云端长连接
//        pms.createClient();

        List<TbDevice> devices = TbDevice.dao.find("select * from tb_device where status = '0'");
        for (TbDevice device : devices) {
            if(device.getDeviceType().equals("DS-K5671")||device.getDeviceType().equals("DS-K5671-H")){
                InitDevice.initHc();

                hcNetSDKService.sendAccessRecord(device.getDeviceIp());
            }   else if (device.getDeviceType().equals("DH-ASI728")) {

                //winds 初始化大華设备
                InitDevice.initDh();
                hcNetSDKService.dhSendAccessRecord(device.getDeviceIp());
            }
        }
        System.out.println("海康初始化状态："+Constant.isInitHc);
        System.out.println("大华初始化状态："+Constant.isInitDh);
    }

    public static void main(String[] args) {
        UndertowServer.create(MainConfig.class)
                .configWeb(builder -> {
                    // 配置WebSocket需使用ServerEndpoint注解
                    builder.addWebSocketEndpoint(com.xiaosong.common.websocket.WebSocket.class);
                })
                .start();
        System.out.println("系统服务启动完成.......");
    }
}
