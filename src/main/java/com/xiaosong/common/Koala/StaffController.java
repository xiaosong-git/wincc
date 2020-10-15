package com.xiaosong.common.Koala;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.core.Controller;
import com.jfinal.kit.HttpKit;
import com.xiaosong.common.server.ServerService;
import com.xiaosong.constant.Constants;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbAccessrecord;
import com.xiaosong.model.TbCompanyuser;
import com.xiaosong.model.TbDevice;
import com.xiaosong.util.Base64_2;
import com.xiaosong.util.Control24DeviceUtil;
import com.xiaosong.util.FilesUtils;
import com.xiaosong.util.RetUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

public class StaffController extends Controller {
    private static StaffService srv = StaffService.me;
    private static Logger logger = Logger.getLogger(StaffController.class);
    public static String token = null;

    public void login() {
        try {
            String url = "http://192.168.0.210:80/auth/login";

            NameValuePair[] data = {
                    new NameValuePair("username", "test@megvii.com"),
                    new NameValuePair("password", "kl123456"),
                    new NameValuePair("auth_token", "true")
            };
            String response = "";//要返回的response信息
            HttpClient httpClient = new HttpClient();
            PostMethod postMethod = new PostMethod(url);
            postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            postMethod.addRequestHeader("user-agent", "Koala Admin");
            // 将表单的值放入postMethod中
            postMethod.setRequestBody(data);
            // 执行postMethod
            int statusCode = 0;
            try {
                statusCode = httpClient.executeMethod(postMethod);
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //状态
            int code = 0;
            // HttpClient对于要求接受后继服务的请求，象POST和PUT等不能自动处理转发
            // 301或者302
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                    || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                // 从头中取出转向的地址
                Header locationHeader = postMethod.getResponseHeader("location");
                String location = null;
                if (locationHeader != null) {
                    location = locationHeader.getValue();
                    System.out.println("The page was redirected to:" + location);
                } else {
                    System.err.println("Location field value is null.");
                }
            } else {
                System.out.println("登录状态:" + postMethod.getStatusLine());
                code = postMethod.getStatusLine().getStatusCode();

                try {
                    response = postMethod.getResponseBodyAsString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                postMethod.releaseConnection();
            }
            JSONObject parse = JSON.parseObject(response);
            JSONObject Data = JSON.parseObject(parse.getString("data"));
            token = (String) Data.get("auth_token");

            if (code == 200) {
                logger.info("登录成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "登录成功~"));
            } else {
                logger.error("登录失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "登录失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("登录异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "登录异常~"));
        }
    }

    /**
     * 下发用户
     */
    public void save() {
        try {
            if (token == null) {
                login();
            }
            String url = getPara("url");
            String path = "http://" + url + ":80/subject/file";

            String photo = getPara("photo");
            String name = getPara("name");
            String subject_type = getPara("subject_type");
//            UploadFile file = getFile();
            String start = null;
            String end = null;
            String str = null;
            if (subject_type.equals("1") || subject_type.equals("2")) {
                start = getPara("startDate");
                end = getPara("endDate");
                str = doPost(path, name, subject_type, photo, start, end,token);
            } else {
                str = doPost(path, name, subject_type, photo, start, end,token);
            }

            JSONObject parse = JSONObject.parseObject(str);

            String data = parse.getString("data");
            JSONObject par = JSONObject.parseObject(data);
            String id = par.getString("id");

            Integer code = (Integer) parse.get("code");
            if (code == 0) {
                TbCompanyuser companyuser = getModel(TbCompanyuser.class);
                companyuser.setUserName(name);
                companyuser.setUserId(Integer.valueOf(id));
                String type = null;
                if (subject_type.equals("0")) {
                    //type = "员工";
                    type = "staff";
                } else if (subject_type.equals("1")) {
                    //type = "访客";
                    type = "visitor";
                } else if (subject_type.equals("2")) {
                    //type = "vip";
                    type = "vip";
                } else if (subject_type.equals("3")) {
                    //type = "黄名单";
                    type = "Yellow list";
                }
                companyuser.setRoleType(type);
                companyuser.setReceiveDate(getDate());
                companyuser.setReceiveTime(getTime());
                companyuser.setIdNO("-1");
                companyuser.setCompanyId(Integer.valueOf(id));
                companyuser.setStatus("applySuc");
                companyuser.setIdType("01");
                companyuser.setCurrentStatus("normal");
                companyuser.save();
                logger.info("添加成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "添加成功~"));
            } else {
                logger.error("添加失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("添加异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "添加异常~"));
        }
    }

    /**
     * 删除用户
     */
    public void delete() {
        try {
            if (token == null) {
                login();
            }
            String url = getPara("url");
            String sid = getPara("sid");

            String path = "http://" + url + ":80/subject/" + sid;
            String str = doDelete(path, token);
            JSONObject jsonObject = JSONObject.parseObject(str);
            Integer code = (Integer) jsonObject.get("code");
            if (code == 0) {
                srv.delete(sid);
                logger.info("删除成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "删除成功~"));
            } else {
                logger.error("删除失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除失败~"));
            }
            System.out.println(str);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("删除异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "删除异常~"));
        }
    }

    /**
     * 修改用户
     */
    public void update() {
        try {
            if (token == null) {
                login();
            }
            String url = getPara("url");
            String sid = getPara("sid");
            String name = getPara("name");
            String subject_type = getPara("subject_type");
//            UploadFile file = getFile();
            String photo = getPara("photo");
            String path = "http://" + url + ":80/subject/" + sid;
            doDelete(path, token);
            srv.delete(sid);
            String start = null;
            String end = null;
            String str = null;
            if (subject_type.equals("1") || subject_type.equals("2")) {
                start = get("start");
                end = get("end");
                str = doPost(path, name, subject_type, photo, start, end,token);
            }
            String savePath = "http://" + url + ":80/subject/file";
            str = doPost(savePath, name, subject_type, photo, start, end,token);
            JSONObject jsonObject = JSONObject.parseObject(str);
            Integer code = (Integer) jsonObject.get("code");

            String data = jsonObject.getString("data");
            JSONObject par = JSONObject.parseObject(data);
            String id = par.getString("id");
            if (code == 0) {
                TbCompanyuser companyuser = getModel(TbCompanyuser.class);
                companyuser.setUserName(name);
                companyuser.setUserId(Integer.valueOf(id));
                String type = null;
                if (subject_type.equals("0")) {
                    //type = "员工";
                    type = "staff";
                } else if (subject_type.equals("1")) {
                    //type = "访客";
                    type = "visitor";
                } else if (subject_type.equals("2")) {
                    //type = "vip";
                    type = "vip";
                } else if (subject_type.equals("3")) {
                    //type = "黄名单";
                    type = "Yellow list";
                }
                companyuser.setRoleType(type);
                companyuser.setReceiveDate(getDate());
                companyuser.setReceiveTime(getTime());
                companyuser.setIdNO("-1");
                companyuser.setCompanyId(Integer.valueOf(id));
                companyuser.setStatus("applySuc");
                companyuser.setIdType("01");
                companyuser.setCurrentStatus("normal");
                companyuser.save();
                logger.info("修改成功~");
                renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "修改成功~"));
            } else {
                logger.error("修改失败~");
                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "修改失败~"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("修改异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "修改异常~"));
        }
    }

    /**
     * 查询所有
     */
    public void index() {
        try {
            if (token == null) {
                login();
            }
            OkHttpClient client = new OkHttpClient();
            int current = getInt("current");
            int size = getInt("size");
            String username = getPara("username");
            String url = getPara("url");
            int index = (current - 1) * size;

            Request request = null;
            if (username == null) {
                request = new Request.Builder()
                        .url("http://" + url + "/event/events")
                        .get()
                        .addHeader("Authorization", token)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
            } else {
                request = new Request.Builder()
                        .url("http://" + url + "/event/events?user_name=" + username)
                        .get()
                        .addHeader("Authorization", token)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();
            }

            Response response = client.newCall(request).execute();
            String res = response.body().string();
            JSONObject parse = JSONObject.parseObject(res);
            JSONArray jsonArray = JSON.parseArray(parse.getString("data"));
            List<Object> list = new ArrayList<Object>();

            for (int i = index; i < ((index + size)); i++) {
                String name = null;
                String id = null;
                String subject_type = null;
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String photo = jsonObject.getString("photo");
                long timestamp = Long.parseLong(jsonObject.getString("timestamp"));
                String screen = jsonObject.getString("screen");
                JSONObject jsonObject1 = JSONObject.parseObject(screen);
                String network_switcher = jsonObject1.getString("network_switcher");

                String subject = jsonObject.getString("subject");
                String result1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp * 1000));
                if (subject == null) {
                    JSONObject jo = new JSONObject();

                    jo.put("deviceIp", network_switcher);
                    jo.put("name", "陌生人");
                    jo.put("date", result1);
                    jo.put("photo", url + photo);
                    list.add(jo);
                    System.out.println(network_switcher + "_陌生人_" + result1 + "_" + url + photo);
                } else {
                    JSONObject jo = new JSONObject();

                    JSONObject jsonObject2 = JSONObject.parseObject(subject);
                    name = jsonObject2.getString("name");
                    String type = null;
                    subject_type = jsonObject2.getString("subject_type");
                    if (subject_type.equals("0")) {
                        type = "员工";
                    } else if (subject_type.equals("1")) {
                        type = "访客";
                    } else if (subject_type.equals("2")) {
                        type = "vip";
                    } else if (subject_type.equals("3")) {
                        type = "黄名单";
                    }
                    id = jsonObject2.getString("id");
                    jo.put("id", id);
                    jo.put("deviceIp", network_switcher);
                    jo.put("name", name);
                    jo.put("type", type);
                    jo.put("date", result1);
                    jo.put("photo", url + photo);
                    list.add(jo);
                    System.out.println(id + "_" + network_switcher + "_" + name + "_" + type + "" + result1 + "_" + url + photo);
                }

            }
            logger.info("查询成功~");
            renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, list, list.size()));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("查询异常~");
            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "查询异常~"));
        }
    }

    //获取 实时通行记录
    public void acc() {
        try {
            String data = HttpKit.readData(getRequest());
            JSONObject jsonObject = JSONObject.parseObject(data);
            String id = String.valueOf(jsonObject.get("subject_id"));

            //白名单操作
            if (!id.equals("null") && !id.equals("-1")) {
                //继电器开门
                Control24DeviceUtil.controlDevice("192.168.0.254",8080, "OUT4", "s");
                long timestamp = Long.parseLong(jsonObject.getString("timestamp"));
                String photo = String.valueOf(jsonObject.get("photo"));
                String device = String.valueOf(jsonObject.get("screen_token"));
                List<TbDevice> devices = srv.findAllDevice();
                String dev = null;
                for (TbDevice tbDevice : devices) {
                    if(device.equals(tbDevice.getDeviceName())){
                        dev=tbDevice.getDeviceIp();
                    }
                }

                String result1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp * 1000));
                String[] split = result1.split("\\ ");
                String date = split[0]; //时间
                String time = split[1]; //时间

                //根据id查询用户
                TbCompanyuser companyuser = srv.findUserId(id);

                byte[] photoKey = Base64_2.decode(photo);
                String fileName = companyuser.getUserName() + "_" + timestamp * 1000 + ".jpg";
                FilesUtils.getFileFromBytes(photoKey, Constants.AccessRecPath1, fileName);
                //文件改名
//                String c=file.getParent();
//                File mm=new File(c+"/"+ Math.random()+".jpg");
//                file.renameTo(mm);
                //保存通行记录
                TbAccessrecord accessrecord = getModel(TbAccessrecord.class);
                accessrecord.setOrgCode(ServerService.me.findByOrgCode());
                accessrecord.setPospCode(ServerService.me.findPospCode());
                accessrecord.setScanDate(date);
                accessrecord.setScanTime(time);
                accessrecord.setTurnOver("in");
                accessrecord.setDeviceType("FACE");
                accessrecord.setUserName(companyuser.getUserName());
                accessrecord.setCardNO(id);
                accessrecord.setDeviceIp(dev);
                accessrecord.setIdCard(companyuser.getIdNO());
                accessrecord.setUserType(companyuser.getRoleType());
                accessrecord.setPhoto("/Recored/" + fileName);
                accessrecord.setIsSendFlag("F");
                boolean save = accessrecord.save();
                if (save) {
                    logger.info("保存通行成功~");
                    renderNull();
                } else {
                    logger.error("保存通行失败~");
                    renderNull();
                }

            } else {
                logger.info("非白名单人员~");
                renderNull();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("获取异常~");
            renderNull();
        }
    }

//    /**
//     *  登录 Post提交
//     * url 要提交到的位置
//     * data 例如：NameValuePair[] data = {new NameValuePair("key", "nike"),new NameValuePair("proClass", "")};
//     *
//     * @return 返回HTML代码
//     */
//    public int methodPost() {
//
//        String url = "http://192.168.0.210:80/auth/login";
//
//        NameValuePair[] data = {
//                new NameValuePair("username", "test@megvii.com"),
//                new NameValuePair("password", "kl123456"),
//                new NameValuePair("auth_token", "true")
//        };
//        String response = "";//要返回的response信息
//        HttpClient httpClient = new HttpClient();
//        PostMethod postMethod = new PostMethod(url);
//        postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
//        postMethod.addRequestHeader("user-agent", "Koala Admin");
//        // 将表单的值放入postMethod中
//        postMethod.setRequestBody(data);
//        // 执行postMethod
//        int statusCode = 0;
//        try {
//            statusCode = httpClient.executeMethod(postMethod);
//        } catch (HttpException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //状态
//        int code = 0;
//        // HttpClient对于要求接受后继服务的请求，象POST和PUT等不能自动处理转发
//        // 301或者302
//        if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
//                || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
//            // 从头中取出转向的地址
//            Header locationHeader = postMethod.getResponseHeader("location");
//            String location = null;
//            if (locationHeader != null) {
//                location = locationHeader.getValue();
//                System.out.println("The page was redirected to:" + location);
//                code = methodPost();//用跳转后的页面重新请求。
//            } else {
//                System.err.println("Location field value is null.");
//            }
//        } else {
//            System.out.println("登录状态:" + postMethod.getStatusLine());
//            code = postMethod.getStatusLine().getStatusCode();
//
//            try {
//                response = postMethod.getResponseBodyAsString();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            postMethod.releaseConnection();
//        }
//        JSONObject parse = JSON.parseObject(response);
//        JSONObject Data = JSON.parseObject(parse.getString("data"));
//        token = (String) Data.get("auth_token");
//        return code;
//    }

    /**
     * Post提交
     * url 要提交到的位置
     * data 例如：NameValuePair[] data = {new NameValuePair("key", "nike"),new NameValuePair("proClass", "")};
     *
     * @return 返回HTML代码
     */
    public String methodPost(String url, NameValuePair[] data) throws Exception {

        String response = "";//要返回的response信息
        HttpClient httpClient = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        postMethod.addRequestHeader("Authorization", token);
        postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
        // 将表单的值放入postMethod中
        postMethod.setRequestBody(data);

        // 执行postMethod
        int statusCode = 0;
        try {
            statusCode = httpClient.executeMethod(postMethod);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // HttpClient对于要求接受后继服务的请求，象POST和PUT等不能自动处理转发
        // 301或者302
        if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
            // 从头中取出转向的地址
            Header locationHeader = postMethod.getResponseHeader("location");
            String location = null;
            if (locationHeader != null) {
                location = locationHeader.getValue();
                System.out.println("The page was redirected to:" + location);
                response = methodPost(location, data);//用跳转后的页面重新请求。
            } else {
                System.err.println("Location field value is null.");
            }
        } else {
            System.out.println("请求状态:" + postMethod.getStatusLine());

            try {
                response = postMethod.getResponseBodyAsString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            postMethod.releaseConnection();
        }
        return response;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
//        String url = "http://192.168.0.210:80/auth/login";
//
//        NameValuePair[] data = {
//                new NameValuePair("username", "test@megvii.com"),
//                new NameValuePair("password", "kl123456"),
//                new NameValuePair("auth_token", "true")
//        };
//        String response = "";//要返回的response信息
//        HttpClient httpClient = new HttpClient();
//        PostMethod postMethod = new PostMethod(url);
//        postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
//        postMethod.addRequestHeader("user-agent", "Koala Admin");
//        // 将表单的值放入postMethod中
//        postMethod.setRequestBody(data);
//        // 执行postMethod
//        int statusCode = 0;
//        try {
//            statusCode = httpClient.executeMethod(postMethod);
//        } catch (HttpException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //状态
//        int code = 0;
//        // HttpClient对于要求接受后继服务的请求，象POST和PUT等不能自动处理转发
//        // 301或者302
//        if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
//                || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
//            // 从头中取出转向的地址
//            Header locationHeader = postMethod.getResponseHeader("location");
//            String location = null;
//            if (locationHeader != null) {
//                location = locationHeader.getValue();
//                System.out.println("The page was redirected to:" + location);
//            } else {
//                System.err.println("Location field value is null.");
//            }
//        } else {
//            System.out.println("登录状态:" + postMethod.getStatusLine());
//            code = postMethod.getStatusLine().getStatusCode();
//
//            try {
//                response = postMethod.getResponseBodyAsString();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            postMethod.releaseConnection();
//        }
//        JSONObject parse = JSON.parseObject(response);
//        JSONObject Data = JSON.parseObject(parse.getString("data"));
//        token = (String) Data.get("auth_token");

        String url1 = "http://192.168.0.210:80/subject/162";
        File file = new File("E:\\sts-space\\photoCache\\staff\\雷磊1.jpg");
//        OkHttpUtil okHttpUtil = new OkHttpUtil();
//
//

//        NameValuePair[] data1 = {
//                new NameValuePair("name", "雷磊"),
//                new NameValuePair("subject_type", "0"),
//                new NameValuePair("photo", file.getAbsolutePath()),
//
//        };
//        StaffController StaffController = new StaffController();
////        StaffController.methodPost();
//        String response1 = StaffController.methodPost(url1, data1);
//        System.out.println(response1);

//        String s = doPut(url1,"雷磊","0" ,"E:\\sts-space\\photoCache\\staff\\雷磊1.jpg");
//        System.out.println(s);
//        JSONObject jsonObject = JSONObject.parseObject(s);
//        System.out.println(jsonObject);


//        Map<String, String> map = new HashMap<String, String>();
//        map.put("user_name ","张三");
//        String result = get(map, "http://192.168.0.210/event/events");
//        JSONObject jsonObject = JSONObject.parseObject(result);
//        System.out.println(jsonObject);


        // JSONArray jsonArray2 = JSON.parseArray(subject);
//            JSONArray jsonArray1 = JSON.parseArray(jsonArray.getJSONObject(i - 1).getString("subject"));
//            if(jsonArray1==null){
//                System.out.println("陌生人");
//                return;
//            }

    }

    /**
     * 保存用户
     *
     * @param url      路径
     * @param name     姓名
     * @param type     类型
     * @param filePath 照片
     * @return
     */
    public static String doPost(String url, String name, String type, String filePath, String start, String end,String token) {
        String sResponse = "";
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(url);
//            uploadFile.addHeader("Content-Type", "application/x-www-form-urlencoded");
            uploadFile.addHeader("Authorization", token);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            File f = new File(filePath);
            // 把文件加到HTTP的post请求中
            builder.addBinaryBody(
                    "photo",
                    new FileInputStream(f),
                    ContentType.APPLICATION_OCTET_STREAM,
                    f.getName()
            );
            builder.addTextBody("name", name, ContentType.APPLICATION_JSON);
            builder.addTextBody("subject_type", type);
            if (start != null) {
                builder.addTextBody("start_time", start);
                builder.addTextBody("end_time", end);
            }
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();
            sResponse = EntityUtils.toString(responseEntity, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sResponse;
    }

    /**
     * 发送delete请求
     *
     * @param url
     * @param token
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String doDelete(String url, String token) {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(60000).build();
        httpDelete.setConfig(requestConfig);
//        httpDelete.setHeader("Content-type", "application/json");
//        httpDelete.setHeader("DataEncoding", "UTF-8");
//        httpDelete.addHeader("Content-Type","application/x-www-form-urlencoded");
//        httpDelete.addHeader(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
        httpDelete.setHeader("Authorization", token);

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpDelete);
            HttpEntity entity = httpResponse.getEntity();
            String result = EntityUtils.toString(entity);
            return result;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String get(Map<String, String> paramMap, String url) {
        String result = "";
        HttpGet get = new HttpGet(url);
        try {
            get.addHeader("Authorization", "78f27fe7-07e9-46fd-a816-fda50ebdd6e7");
            get.addHeader("Content-Type", "application/x-www-form-urlencoded");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            List<org.apache.http.NameValuePair> params = setHttpParams(paramMap);
            String param = URLEncodedUtils.format(params, "UTF-8");
            get.setURI(URI.create(url + "?" + param));
            HttpResponse response = httpClient.execute(get);
            result = getHttpEntityContent(response);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                result = "服务器异常";
            }
        } catch (Exception e) {
            System.out.println("请求异常");
            throw new RuntimeException(e);
        } finally {
            get.abort();
        }
        return result;
    }

    public static List<org.apache.http.NameValuePair> setHttpParams(Map<String, String> paramMap) {
        List<org.apache.http.NameValuePair> params = new ArrayList<org.apache.http.NameValuePair>();
        Set<Map.Entry<String, String>> set = paramMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return params;
    }

    public static String getHttpEntityContent(HttpResponse response) throws UnsupportedOperationException, IOException {
        String result = "";
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream in = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"));
            StringBuilder strber = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                strber.append(line + '\n');
            }
            br.close();
            in.close();
            result = strber.toString();
        }

        return result;
    }

    //获取 当前时间的 年-月-日
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    //获取 当前时间的 时:分:秒
    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }

    //获取当前时间的  年-月-日  时:分:秒
    private String getDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }
}

