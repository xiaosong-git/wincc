package com.xiaosong.constant;

import com.jfinal.kit.PathKit;

public class Constant {

    /**
     * 是否开发模式-生产是自动加载改为false即可，自动加载生产配置文件
     */
    public final static Boolean DEV_MODE = false;

    /**
     *  定时拉取数据页数 page
     *  定时拉取数据数量 PAGENUMBER
     */
    public final static int page = 1;

    public final static int PAGENUMBER = 10;


    /**
     * 员工初始化地址后缀
     */
    public final static String initUsers ="/visitor/companyUser/findApplyAllSucOrg";

    /**
     * 拉取员工地址后缀
     */
    public final static String pullOrgCompanyUrl ="/visitor/companyUser/findApplySucOrg";

    /**
     * 拉取访客地址后缀
     */
    public final static String newpullStaffUrl = "/visitor/foreign/newFindOrgCode";

    /**
     * 确认拉取访客地址后缀
     */
    public final static String newconfirmReceiveUrl = "/visitor/foreign/newFindOrgCodeConfirm";

    /**
     * 访客、员工照片本地存储路径
     */
    public final static String VisitorPath = PathKit.getWebRootPath() + "/img/visitor/";
    public final static String StaffPath  = PathKit.getWebRootPath() + "/img/";

    /**
     * 海康SDK初始化标志
     */
    public static boolean isInitHc = false;
    public static boolean isInitDh = false;
}
