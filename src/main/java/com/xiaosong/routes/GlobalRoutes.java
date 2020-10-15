package com.xiaosong.routes;

import com.jfinal.config.Routes;
import com.xiaosong.common.Koala.StaffController;
import com.xiaosong.common.LoginController;
import com.xiaosong.common.accessrecord.AccessRecordController;
import com.xiaosong.common.device.DeviceController;
import com.xiaosong.common.device.DeviceRelatedController;
import com.xiaosong.common.license.LicenseController;
import com.xiaosong.common.network.NetWorkController;
import com.xiaosong.common.personnel.PersonnelController;
import com.xiaosong.common.server.ServerController;
import com.xiaosong.common.shangweiji.ShangWeiJiServerController;
import com.xiaosong.netty.PMSWebSocketClient;
import com.xiaosong.common.wincc.companyuser.InitCompanyUsersController;

public class GlobalRoutes extends Routes {

    @Override
    public void config() {
        /**配置说明 controllerKey为Controller的前缀，如UserController的前缀为User
         *   配置路径                                实际访问路径
         * controllerKey        YourController.index()
         * controllerKey/method YourController.method()
         * controllerKey/method/v0-v1 YourController.method()
         * controllerKey/v0-v1 YourController.index()，所带 url 参数值为：v0-v1
         */

        //该处还可配置route级别的拦截器，对N个含有共同拦截器的控制层实现统一配置，减少代码冗余

        //登录路由
        this.add("/login", LoginController.class);
        //设备路由
        this.add("/device", DeviceController.class);
        //设备关联路由
        this.add("/dr", DeviceRelatedController.class);
        //通行记录路由
        this.add("/acc", AccessRecordController.class);
        //人员路由
        this.add("/per", PersonnelController.class);
        //大楼服务器路由
        this.add("/server", ServerController.class);
        //网络配置路由
        this.add("/netWork", NetWorkController.class);
        //license配置路由
        this.add("/license", LicenseController.class);
        this.add("/shangweiji", ShangWeiJiServerController.class);
        //旷世人员管理路由
        this.add("/staff", StaffController.class);
        //pms停车场路由
        this.add("/pms", PMSWebSocketClient.class);

        add("initCompanyUsers", InitCompanyUsersController.class);
    }
}
