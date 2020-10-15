package com.xiaosong.util;

import com.jfinal.json.Json;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回值封装，常用于业务层需要多个返回值
 */
@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class RetUtil extends HashMap{
	private static final String CODE = "code";
	private static final String MSG = "msg";
	private static final String SIZE = "size";
	private static final String SUCCESS = "success";
	private static final String SUCCESS_CODE = "200";
	private static final String FAIL_CODE = "501";
	private static final String SUCCESS_MSG = "操作成功";
	private static final String FAIL_MSG = "操作失败";
	
	
	public static RetUtil ok() {
		return new RetUtil().setOk();
	}
	public static RetUtil ok(Object value) {
		return new RetUtil().setOk(value);
	}
	public static RetUtil ok(int code, Object value) {
		return new RetUtil().setOk(code,value);
	}
	
	public static RetUtil ok(Object key, Object value) {
		return ok().set(key, value);
	}
	
	public static RetUtil fail() {
		return new RetUtil().setFail();
	}
	
	public static RetUtil fail(int code, Object msg) {
		return new RetUtil().setFail(code,msg);
	}
	
	public static RetUtil ok(int code, Object msg, int size) {
		return new RetUtil().setOk(code, msg,size);
	}
	
	public static RetUtil fail(Object msg) {
		return new RetUtil().setFail(msg);
	}
	
	public static RetUtil fail(Object key, Object value) {
		return fail().set(key, value);
	}
	
	public static RetUtil create() {
		return new RetUtil();
	}
	
	public static RetUtil create(Object key, Object value) {
		return new RetUtil().set(key, value);
	}
	
	public boolean success() {
		Boolean success = (Boolean)get(SUCCESS);
		return success != null && success;
	}
	
	/**
	 * 无参返回成功
	 * @return
	 */
	public RetUtil setOk() {
		super.put(SUCCESS, true);
		super.put(CODE, SUCCESS_CODE);
		super.put(MSG, SUCCESS_MSG);
		return this;
	}

	public RetUtil setOk(Object msg) {
		super.put(SUCCESS, true);
		super.put(CODE, SUCCESS_CODE);
		super.put(MSG, msg);
		return this;
	}
	/**
	 * 有参返回成功
	 * @param msg
	 * @return
	 */
	public RetUtil setOk(int code, Object msg) {
		super.put(SUCCESS, true);
		super.put(CODE, code);
		super.put(MSG, msg);
		return this;
	}
	
	/**
	 * 
	 * @param code
	 * @param msg
	 * @return
	 */
	public RetUtil setOk(int code, Object msg, int size) {
		super.put(SUCCESS, true);
		super.put(CODE, code);
		super.put(MSG, msg);
		super.put(SIZE, size);
		return this;
	}
	
	/**
	 * 返回失败
	 * @param msg
	 * @return
	 */
	public RetUtil setFail(Object msg) {
		super.put(SUCCESS, false);
		super.put(CODE, FAIL_CODE);
		super.put(MSG, msg);
		return this;
	}
	
	/**
	 * 返回失败
	 * @param code
	 * @param msg
	 * @return
	 */
	public RetUtil setFail(int code, Object msg) {
		super.put(SUCCESS, false);
		super.put(CODE, code);
		super.put(MSG, msg);
		return this;
	}
	
	/**
	 * 无参返回失败
	 * @return
	 */
	public RetUtil setFail() {
		super.put(SUCCESS, false);
		super.put(CODE, FAIL_CODE);
		super.put(MSG, FAIL_MSG);
		return this;
	}
	
	public RetUtil set(Object key, Object value) {
		super.put(key, value);
		return this;
	}
	
	public RetUtil setCode(int value) {
		super.put(CODE, value);
		return this;
	}
	
	public RetUtil setMsg(String value) {
		super.put(MSG, value);
		return this;
	}
	
	public RetUtil set(Map map) {
		super.putAll(map);
		return this;
	}
	
	public RetUtil set(RetUtil ret) {
		super.putAll(ret);
		return this;
	}
	
	public RetUtil delete(Object key) {
		super.remove(key);
		return this;
	}
	
	public <T> T getAs(Object key) {
		return (T)get(key);
	}
	
	public String getStr(Object key) {
		return (String)get(key);
	}

	public Integer getInt(Object key) {
		return (Integer)get(key);
	}

	public Long getLong(Object key) {
		return (Long)get(key);
	}

	public Boolean getBoolean(Object key) {
		return (Boolean)get(key);
	}
	
	/**
	 * key 存在，并且 value 不为 null
	 */
	public boolean notNull(Object key) {
		return get(key) != null;
	}
	
	/**
	 * key 不存在，或者 key 存在但 value 为null
	 */
	public boolean isNull(Object key) {
		return get(key) == null;
	}
	
	/**
	 * key 存在，并且 value 为 true，则返回 true
	 */
	public boolean isTrue(Object key) {
		Object value = get(key);
		return (value instanceof Boolean && ((Boolean)value == true));
	}
	
	/**
	 * key 存在，并且 value 为 false，则返回 true
	 */
	public boolean isFalse(Object key) {
		Object value = get(key);
		return (value instanceof Boolean && ((Boolean)value == false));
	}
	
	public String toJson() {
		return Json.getJson().toJson(this);
	}
	
	public boolean equals(Object ret) {
		return ret instanceof RetUtil && super.equals(ret);
	}
}
