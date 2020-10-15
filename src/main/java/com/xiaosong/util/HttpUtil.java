package com.xiaosong.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpUtil {


	public static ThirdResponseObj http2Nvp(String url, List<BasicNameValuePair> nvps) throws Exception {
		HttpClient httpClient = new SSLClient();
		HttpPost postMethod = new HttpPost(url);

//		  //链接超时
//        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
//        //读取超时 设置为30秒
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		// HttpResponse resp = null;
		HttpResponse resp = httpClient.execute(postMethod);

		int statusCode = resp.getStatusLine().getStatusCode();

		ThirdResponseObj responseObj = new ThirdResponseObj();
		if (200 == statusCode) {
			responseObj.setCode("success");
			String str = EntityUtils.toString(resp.getEntity(), "UTF-8");
			responseObj.setResponseEntity(str);
		} else {
			responseObj.setCode(statusCode + "");
		}

		return responseObj;
	}

	public static ThirdResponseObj http2Nvp(String url, Map<String, String> map, String encodeType) throws Exception {

		List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();

		for (Map.Entry<String, String> entry : map.entrySet()) {
			nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}

		HttpClient httpClient = new SSLClient();
		HttpPost postMethod = new HttpPost(url);

//		  //链接超时
//        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
//        //读取超时
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 300000);

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, encodeType));
		// HttpResponse resp = null;
		HttpResponse resp = httpClient.execute(postMethod);

		int statusCode = resp.getStatusLine().getStatusCode();

		ThirdResponseObj responseObj = new ThirdResponseObj();
		if (200 == statusCode) {
			responseObj.setCode("success");
			String str = EntityUtils.toString(resp.getEntity(), encodeType);
			responseObj.setResponseEntity(str);
		} else {
			responseObj.setCode(statusCode + "");
		}

		return responseObj;
	}

	public static ThirdResponseObj http2Se(String url, StringEntity entity, String encodingType)
			throws Exception {

		try {
			HttpClient httpClient = new SSLClient();
			HttpPost postMethod = new HttpPost(url);

			postMethod.setEntity(entity);
			// HttpResponse resp = null;
			HttpResponse resp = httpClient.execute(postMethod);

			int statusCode = resp.getStatusLine().getStatusCode();

			ThirdResponseObj responseObj = new ThirdResponseObj();
			if (200 == statusCode) {
				responseObj.setCode("success");
				String str = EntityUtils.toString(resp.getEntity(), encodingType);
				responseObj.setResponseEntity(str);
			} else {
				responseObj.setCode(statusCode + "");
			}
			return responseObj;
		} catch (Exception e) {
			throw new Exception("连接"+url+"异常");
		}

	}

	private static final ContentType CONTENT_TYPE_APPLICATION_FORM_URLENCODED = ContentType
			.create("application/x-www-form-urlencoded", "UTF-8");
	private static final String REG_EXP_CONTENT_TYPE = "([\\w*?|\\*?]/[\\w*?|\\*?])(;\\s*)(charset=(\\w+))";
	private static Pattern PATTERN_CONTENT_TYPE = Pattern.compile(REG_EXP_CONTENT_TYPE);



}
