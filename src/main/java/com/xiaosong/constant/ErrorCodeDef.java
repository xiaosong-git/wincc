package com.xiaosong.constant;
/**
 * 
 * @author wgm
 * create by 2019-11-05
 * 系统错误码表-有其他需要继续往后加
 */
public class ErrorCodeDef {
	
	/**
	 * 0000 - 二维码
	 */
	public static final int CODE = 0000;

	/**
	 * 200 - 正常
	 */
	public static final int CODE_NORMAL = 200;
	/**
	 * 500 -系统错误
	 */
	public static final int CODE_ERROR = 500;
	/**
	 * 404 
	 */
	public static final int CODE_404 = 404;
	/**
	 * 无访问权限
	 */
	public static final int CODE_NO_AUTH = 401;
	/**
	 * 未登录
	 */
	public static final int CODE_NO_LOGIN = 10001;
	/**
	 * session 失效
	 */
	public static final int CODE_LOGIN_EXPIRE = 10002;

}
