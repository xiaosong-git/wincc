package com.xiaosong.interceptor;

import com.jfinal.aop.Clear;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;

/**
 * 登录session过滤器
 * @author Administrator
 *
 */
@Clear(AuthInterceptor.class)
public class LoginInterceptor implements Interceptor {

	@Override
	public void intercept(Invocation inv) {
		Controller con = inv.getController();
//		String sessionId = con.getHeader(Constants.ADMIN_SESSION_ID);
//		if (StrKit.isBlank(sessionId)) {
////			con.renderJson(RetKit.fail(RetConstant.CODE_NO_LOGIN, "未登錄"));
//			return;
//		}
//		SysAccount account = LoginService.me.getAccountCacheWithSessionId(sessionId);
//		if (account == null) {
//			account = LoginService.me.loginWithSessionId(sessionId, Ipkit.getRealIp(con.getRequest()));
//		}
//		if (account == null) {
//			con.renderJson(RetKit.fail(RetConstant.CODE_LOGIN_EXPIRE, "會話過期，請重新登陸"));
//			return;
//		}
		inv.invoke();
	}

}
