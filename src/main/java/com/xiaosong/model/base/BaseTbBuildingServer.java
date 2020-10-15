package com.xiaosong.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseTbBuildingServer<M extends BaseTbBuildingServer<M>> extends Model<M> implements IBean {

	public M setServerId(Integer serverId) {
		set("serverId", serverId);
		return (M)this;
	}

	public Integer getServerId() {
		return getInt("serverId");
	}

	public M setOrgCode(String orgCode) {
		set("orgCode", orgCode);
		return (M)this;
	}

	public String getOrgCode() {
		return getStr("orgCode");
	}

	public M setPospCode(String pospCode) {
		set("pospCode", pospCode);
		return (M)this;
	}

	public String getPospCode() {
		return getStr("pospCode");
	}

	public M setNetType(String netType) {
		set("netType", netType);
		return (M)this;
	}

	public String getNetType() {
		return getStr("netType");
	}

	public M setKey(String key) {
		set("key", key);
		return (M)this;
	}

	public String getKey() {
		return getStr("key");
	}

	public M setServerIp(String serverIp) {
		set("serverIp", serverIp);
		return (M)this;
	}

	public String getServerIp() {
		return getStr("serverIp");
	}

	public M setServerPort(String serverPort) {
		set("serverPort", serverPort);
		return (M)this;
	}

	public String getServerPort() {
		return getStr("serverPort");
	}

	public M setServer2Ip(String server2Ip) {
		set("server2Ip", server2Ip);
		return (M)this;
	}

	public String getServer2Ip() {
		return getStr("server2Ip");
	}

	public M setServer2Port(String server2Port) {
		set("server2Port", server2Port);
		return (M)this;
	}

	public String getServer2Port() {
		return getStr("server2Port");
	}

	public M setStartDate(String startDate) {
		set("startDate", startDate);
		return (M)this;
	}

	public String getStartDate() {
		return getStr("startDate");
	}

	public M setEndDate(String endDate) {
		set("endDate", endDate);
		return (M)this;
	}

	public String getEndDate() {
		return getStr("endDate");
	}

	public M setVisitorStartDate(String visitorStartDate) {
		set("visitorStartDate", visitorStartDate);
		return (M)this;
	}

	public String getVisitorStartDate() {
		return getStr("visitorStartDate");
	}

	public M setVisitorEndDate(String visitorEndDate) {
		set("visitorEndDate", visitorEndDate);
		return (M)this;
	}

	public String getVisitorEndDate() {
		return getStr("visitorEndDate");
	}

	public M setStopStartDate(String stopStartDate) {
		set("stopStartDate", stopStartDate);
		return (M)this;
	}

	public String getStopStartDate() {
		return getStr("stopStartDate");
	}

	public M setStopEndDate(String stopEndDate) {
		set("stopEndDate", stopEndDate);
		return (M)this;
	}

	public String getStopEndDate() {
		return getStr("stopEndDate");
	}

	public M setQrcodeType(String qrcodeType) {
		set("qrcodeType", qrcodeType);
		return (M)this;
	}

	public String getQrcodeType() {
		return getStr("qrcodeType");
	}

	public M setIsFlagCompany(Integer isFlagCompany) {
		set("isFlagCompany", isFlagCompany);
		return (M)this;
	}

	public Integer getIsFlagCompany() {
		return getInt("isFlagCompany");
	}

	public M setSectionId(Integer sectionId) {
		set("sectionId", sectionId);
		return (M)this;
	}

	public Integer getSectionId() {
		return getInt("sectionId");
	}

}