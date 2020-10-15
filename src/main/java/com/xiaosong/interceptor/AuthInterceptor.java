package com.xiaosong.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import org.apache.log4j.BasicConfigurator;

/**
 * 权限过滤器
 *
 * @author Administrator
 */
public class AuthInterceptor implements Interceptor {
    static {
        //自动快速地使用缺省Log4j环境。
        BasicConfigurator.configure();
    }
    @Override
    public void intercept(Invocation inv) {
        Controller controller = inv.getController();
        //inv.invoke();
        controller.getResponse().addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS,DELETE,PUT");
        controller.getResponse().addHeader("Access-Control-Allow-Headers", "content-type,authorization");
        controller.getResponse().addHeader("Access-Control-Allow-Origin", "*");
        controller.getResponse().addHeader("Access-Control-Request-Headers", "authorization");
        if (!"OPTIONS".equals(inv.getController().getRequest().getMethod())) {

//            String s =  CacheKit.get("xiaosong", "admin");
//            if(s==null){
//                Logger.getLogger(AuthInterceptor.class).info("用户未登录.");
//                controller.renderJson(RetUtil.fail(ErrorCodeDef.CODE_NO_LOGIN, "用户未登录."));
//            }else{
//                inv.invoke();
//            }

            inv.invoke();
        } else {
            inv.getController().renderNull();
        }

    }
}