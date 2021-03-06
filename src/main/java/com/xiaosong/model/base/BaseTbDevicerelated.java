package com.xiaosong.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseTbDevicerelated<M extends BaseTbDevicerelated<M>> extends Model<M> implements IBean {

	public M setId(Integer id) {
		set("id", id);
		return (M)this;
	}

	public Integer getId() {
		return getInt("id");
	}

	public M setFaceIP(String faceIP) {
		set("faceIP", faceIP);
		return (M)this;
	}

	public String getFaceIP() {
		return getStr("faceIP");
	}

	public M setQRCodeIP(String QRCodeIP) {
		set("QRCodeIP", QRCodeIP);
		return (M)this;
	}

	public String getQRCodeIP() {
		return getStr("QRCodeIP");
	}

	public M setRelayIP(String relayIP) {
		set("relayIP", relayIP);
		return (M)this;
	}

	public String getRelayIP() {
		return getStr("relayIP");
	}

	public M setRelayPort(String relayPort) {
		set("relayPort", relayPort);
		return (M)this;
	}

	public String getRelayPort() {
		return getStr("relayPort");
	}

	public M setRelayOUT(String relayOUT) {
		set("relayOUT", relayOUT);
		return (M)this;
	}

	public String getRelayOUT() {
		return getStr("relayOUT");
	}

	public M setContralFloor(String contralFloor) {
		set("contralFloor", contralFloor);
		return (M)this;
	}

	public String getContralFloor() {
		return getStr("contralFloor");
	}

	public M setTurnOver(String turnOver) {
		set("turnOver", turnOver);
		return (M)this;
	}

	public String getTurnOver() {
		return getStr("turnOver");
	}

	public M setAddr(String addr) {
		set("addr", addr);
		return (M)this;
	}

	public String getAddr() {
		return getStr("addr");
	}

	public M setExpt2(String expt2) {
		set("expt2", expt2);
		return (M)this;
	}

	public String getExpt2() {
		return getStr("expt2");
	}

}
