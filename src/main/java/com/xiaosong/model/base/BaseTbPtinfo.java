package com.xiaosong.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseTbPtinfo<M extends BaseTbPtinfo<M>> extends Model<M> implements IBean {

	public M setDeviceName(String deviceName) {
		set("deviceName", deviceName);
		return (M)this;
	}

	public String getDeviceName() {
		return getStr("deviceName");
	}

	public M setDeviceIP(String deviceIP) {
		set("deviceIP", deviceIP);
		return (M)this;
	}

	public String getDeviceIP() {
		return getStr("deviceIP");
	}

	public M setOrgCode(String orgCode) {
		set("orgCode", orgCode);
		return (M)this;
	}

	public String getOrgCode() {
		return getStr("orgCode");
	}

	public M setPingStatus(String pingStatus) {
		set("pingStatus", pingStatus);
		return (M)this;
	}

	public String getPingStatus() {
		return getStr("pingStatus");
	}

	public M setPingavg(Double pingavg) {
		set("pingavg", pingavg);
		return (M)this;
	}

	public Double getPingavg() {
		return getDouble("pingavg");
	}

	public M setPingloss(String pingloss) {
		set("pingloss", pingloss);
		return (M)this;
	}

	public String getPingloss() {
		return getStr("pingloss");
	}

	public M setTelStatus(String telStatus) {
		set("telStatus", telStatus);
		return (M)this;
	}

	public String getTelStatus() {
		return getStr("telStatus");
	}

	public M setFreshTime(String freshTime) {
		set("freshTime", freshTime);
		return (M)this;
	}

	public String getFreshTime() {
		return getStr("freshTime");
	}

	public M setCpu(String cpu) {
		set("cpu", cpu);
		return (M)this;
	}

	public String getCpu() {
		return getStr("cpu");
	}

	public M setMemory(String memory) {
		set("memory", memory);
		return (M)this;
	}

	public String getMemory() {
		return getStr("memory");
	}

	public M setLongStatus(String longStatus) {
		set("longStatus", longStatus);
		return (M)this;
	}

	public String getLongStatus() {
		return getStr("longStatus");
	}

	public M setExpt1(String expt1) {
		set("expt1", expt1);
		return (M)this;
	}

	public String getExpt1() {
		return getStr("expt1");
	}

	public M setExpt2(String expt2) {
		set("expt2", expt2);
		return (M)this;
	}

	public String getExpt2() {
		return getStr("expt2");
	}

}
