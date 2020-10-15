package com.xiaosong.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseTbFailreceive<M extends BaseTbFailreceive<M>> extends Model<M> implements IBean {

	public M setFailedId(Integer failedId) {
		set("failedId", failedId);
		return (M)this;
	}

	public Integer getFailedId() {
		return getInt("failedId");
	}

	public M setFaceIp(String faceIp) {
		set("faceIp", faceIp);
		return (M)this;
	}

	public String getFaceIp() {
		return getStr("faceIp");
	}

	public M setUserName(String userName) {
		set("userName", userName);
		return (M)this;
	}

	public String getUserName() {
		return getStr("userName");
	}

	public M setIdCard(String idCard) {
		set("idCard", idCard);
		return (M)this;
	}

	public String getIdCard() {
		return getStr("idCard");
	}

	public M setUserType(String userType) {
		set("userType", userType);
		return (M)this;
	}

	public String getUserType() {
		return getStr("userType");
	}

	public M setReceiveTime(String receiveTime) {
		set("receiveTime", receiveTime);
		return (M)this;
	}

	public String getReceiveTime() {
		return getStr("receiveTime");
	}

	public M setReceiveFlag(String receiveFlag) {
		set("receiveFlag", receiveFlag);
		return (M)this;
	}

	public String getReceiveFlag() {
		return getStr("receiveFlag");
	}

	public M setVisitorUUID(String visitorUUID) {
		set("visitorUUID", visitorUUID);
		return (M)this;
	}

	public String getVisitorUUID() {
		return getStr("visitorUUID");
	}

	public M setOpera(String opera) {
		set("opera", opera);
		return (M)this;
	}

	public String getOpera() {
		return getStr("opera");
	}

	public M setDownNum(Integer downNum) {
		set("downNum", downNum);
		return (M)this;
	}

	public Integer getDownNum() {
		return getInt("downNum");
	}

}
