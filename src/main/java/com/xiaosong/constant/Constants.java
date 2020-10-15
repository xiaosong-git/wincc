package com.xiaosong.constant;

import com.jfinal.kit.PathKit;

/**
 * @author wgm
 * create by 2019-11-05
 * 系统常量
 */
public class Constants {

    /**
     * 默认上传临时文件夹 PathKit.getWebRootPath() +"/upload/temp";
     */
    //winds
    public static final String BASE_UPLOAD_PATH = PathKit.getWebRootPath() + "/img/";
    //linux
	public static final String BASE_UPLOAD_PATH1 = PathKit.getWebRootPath() +"/file/";


    /**
     * 默认下载临时文件夹 PathKit.getWebRootPath() +"/download/temp";
     */
    public static final String BASE_DOWNLOAD_PATH = PathKit.getWebRootPath() + "/download/";
    public static final String BASE_DOWNLOAD_PATH1 = PathKit.getWebRootPath() + "/download/";


    /**
     * 账号密码管理session 放入缓存session中
     */
    public final static String SYS_ACCOUNT = "SYS_ACCOUNT";
    /**
     * 权限缓存也放入session
     */
    public final static String SYS_ROLE_MENU = "SYS_ROLE_MENU";

    /**
     * 是否开发模式-生产是自动加载改为false即可，自动加载生产配置文件
     */
    public final static Boolean DEV_MODE = false;

    //设备登录条件
    public static String deviceLoginName = "admin";          //设备登录名
    public static String deviceLoginPassWord = "wgmhao123";  //设备登录密码
//    public static String deviceLoginPassWord = "admin123456"; //8号楼 人脸设备登录密码

    public static String deviceLoginName1 = "admin"; //8号楼 闸机登录账户
    public static String deviceLoginPassWord1 = "zhyqglzx123456"; //8号楼 闸机登录密码
    public static String deviceGate = "10.18.25.120";
    public static String currentStatus = "currentStatus";  //用户状态

//  tomcat
//    public static String VisitorPath = PathKit.getWebRootPath() + "/img/visitor/";      //Linux系统文件存储路径 访客
//    public static String StaffPath = PathKit.getWebRootPath() + "/img/";                //Linux系统文件存储路径 员工
//    public static String StaffPath = PathKit.getWebRootPath() + "/src/main/webapp/img/";    //winds系统文件存储路径 员工
//    	public static String AccessRecPath = "C:\\tomcat\\webapps1\\Recored";    //数据记录路径
    //public static String AccessRecPath = "/usr/tomcat/apache-tomcat-8.5.43/webapps1/Recored";    //tomcat数据记录路径

// jar
    public static String VisitorPath = PathKit.getWebRootPath() + "/img/visitor/";      //Linux系统文件存储路径 访客
    public static String StaffPath = PathKit.getWebRootPath() + "/img/";                //Linux系统文件存储路径 员工
//    public static String AccessRecPath = PathKit.getWebRootPath() + "/src/main/webapp/Recored";  //winds数据记录路径              //Linux系统文件存储路径 员工
    public static String AccessRecPath = PathKit.getWebRootPath()+"/Recored";    //数据记录路径
    public static String AccessRecPath1 = PathKit.getWebRootPath()+"/Recored/";    //数据记录路径

    public static String idNo = "idNo";
    public static String userName = "username";
    public static String pullOrgCompanyUrl = "companyUser/findApplySucOrg"; //新增员工接口
    public static String newpullStaffUrl = "foreign/newFindOrgCode";         //新的访客拉取接口
//    public static String newpullStaffUrl = "foreign/findOrgCode";         //访客拉取接口
    public static final String page = "1";
    public static final int PAGENUMBER = 10;

    public static String newconfirmReceiveUrl = "foreign/newFindOrgCodeConfirm"; //新访客数据确认接收接口
    public static String accessRecordByBatch = "/goldccm-imgServer/inAndOut/save";

    public static final String baseURl = "http://47.96.71.163:8082/visitor/";  //生产API
    public static String qrcode = "qrcode/otherQrcode";   //二维码数据接收接口


}
