package com.xiaosong.common;

import com.jfinal.core.Controller;
import com.jfinal.kit.HttpKit;
import com.jfinal.plugin.ehcache.CacheKit;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.util.RetUtil;
import org.apache.log4j.Logger;
import org.junit.Test;

//@Clear(AuthInterceptor.class)
public class LoginController extends Controller {
    private static Logger logger = Logger.getLogger(LoginController.class.getName());

    /**
     * 登录用户
     */
    public void index() {
        String username = getPara("username");
        String password = getPara("password");
        if (!username.equals("admin")) {
            logger.error("用户名不正确~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "用户名不正确~"));
        } else if (!password.equals("123456")) {
            logger.error("密码不正确~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "密码不正确~"));
        } else {
            //CacheKit.put("xiaosong","admin",username);
            logger.info("登录成功~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "登录成功~"));
        }
    }

    /**
     * 退出
     */
    public void del(){
        try {
            CacheKit.remove("xiaosong","admin");
            logger.info("用户已退出~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "用户已退出~"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void aaa(){
        String s = HttpKit.readData(getRequest());
        String a = getPara("a");
        String b = getPara("b");
        String c = getPara("c");
        String d = getPara("d");
    }
}