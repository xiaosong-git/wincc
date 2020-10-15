package com.xiaosong.util;

import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class OkHttpUtil {
    private static Logger logger = Logger.getLogger(OkHttpUtil.class);

    public interface OnNetworkListen {
        public void onSuccess(String networkResult);

        public void onFailer();
    }


    private String orgCode = "ifc";

    private OkHttpClient okHttpClient = new OkHttpClient();

    /**
     * get(同步)
     *
     * @param url 请求的url
     * @return
     */

    public String get(String url) throws Exception {

        String responseBody = "";


        Request request = new Request
                .Builder()
                .url(url)
                .build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return response.body().string();
            }
        } catch (Throwable throwable) {
            logger.error(url + "网络异常");
//        	String keysign = towerInforService.findOrgId()+towerInforService.findPospCode()+towerInforService.findKey();
            //logger.sendErrorLog(orgCode, "连接"+url+"网络异常", "","网络错误", Constants.errorLogUrl,keysign);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseBody;
    }

    public String get(String url, Map<String, String> queries) throws Exception {

        String responseBody = "";
        StringBuffer sb = new StringBuffer(url);

        if (queries != null && queries.keySet().size() > 0) {
            boolean firstFlag = true;
            Iterator iterator = queries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry<String, String>) iterator.next();
                if (firstFlag) {
                    sb.append("?" + entry.getKey() + "=" + entry.getValue());
                    firstFlag = false;
                } else {
                    sb.append("&" + entry.getKey() + "=" + entry.getValue());
                }
            }
        }
        Request request = new Request.Builder().url(sb.toString()).build();
        Response response = null;

        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return response.body().string();
            }
        } catch (Throwable throwable) {
//			logger.error(url, "网络异常");
            logger.error(url + "网络异常");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseBody;
    }

    /**
     * post(同步)
     *
     * @param url    请求的url
     * @param params post form 提交的参数
     * @return
     * @throws Exception
     */
    public String post(String url, Map<String, String> params) throws Exception {

        String responseBody = "";
        FormBody.Builder builder = new FormBody.Builder();
        // 添加参数
        if (params != null && params.keySet().size() > 0) {
            for (String key : params.keySet()) {
                builder.add(key, params.get(key));
            }
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return response.body().string();
            }
        } catch (Throwable throwable) {
            logger.error(url + "网络异常");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseBody;
    }

    public String post(String url, String pospCode, String orgCode, String visitId, String inOrOut, String visitDate, String visitTime) throws Exception {

        String responseBody = "";

        RequestBody body = new FormBody.Builder()
                .add("pospCode", pospCode)
                .add("orgCode", orgCode)
                .add("visitId", visitId)
                .add("inOrOut", inOrOut)
                .add("visitDate", visitDate)
                .add("visitTime", visitTime)
                .build();

        Request request = new Request.Builder().url(url).post(body).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();

            if (status == 200) {
                return response.body().string();
            }
        } catch (Throwable throwable) {
            logger.error(url + "网络异常");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseBody;
    }


    /**
     * post(异步)
     *
     * @param url    请求的url
     * @param params post form 提交的参数
     * @return
     */
    public String postAsyn(String url, Map<String, String> params, final OnNetworkListen onNetworkListen) {
        String responseBody = "";
        FormBody.Builder builder = new FormBody.Builder();
        // 添加参数
        if (params != null && params.keySet().size() > 0) {
            for (String key : params.keySet()) {
                builder.add(key, params.get(key));
            }
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        try {
            okHttpClient.newCall(request).enqueue(new Callback() {
                public void onFailure(Call call, IOException e) {
                    if (onNetworkListen != null) {
                        onNetworkListen.onFailer();
                    }
                    call.cancel();
                }

                public void onResponse(Call call, Response response) throws IOException {
                    int status = response.code();
                    if (status == 200) {
                        if (onNetworkListen != null) {
                            onNetworkListen.onSuccess(response.body().string());
                        }
                    }

                    response.close();
                    call.cancel();
                }
            });

        } catch (Throwable throwable) {
            logger.error(url + "网络异常");

        } finally {
        }
        return responseBody;
    }

    /**
     * post 上传文件
     *
     * @param url
     * @param params
     * @param fileType
     * @return
     */
    public String postFile(String url, Map<String, Object> params, String fileType) {
        String responseBody = "";
        MultipartBody.Builder builder = new MultipartBody.Builder();
        // 添加参数
        if (params != null && params.keySet().size() > 0) {
            for (String key : params.keySet()) {
                if (params.get(key) instanceof File) {
                    File file = (File) params.get(key);
                    builder.addFormDataPart(key, file.getName(), RequestBody.create(MediaType.parse(fileType), file));
                    continue;
                }
                builder.addFormDataPart(key, params.get(key).toString());
            }
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return response.body().string();
            }
        } catch (Throwable throwable) {

            logger.error(url + "网络异常");

        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseBody;
    }


}
