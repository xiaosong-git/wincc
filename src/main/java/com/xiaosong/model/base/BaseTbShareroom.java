package com.xiaosong.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseTbShareroom<M extends BaseTbShareroom<M>> extends Model<M> implements IBean {

	public M setRecordId(String recordId) {
		set("recordId", recordId);
		return (M)this;
	}

	public String getRecordId() {
		return getStr("recordId");
	}

	public M setRoomId(Integer roomId) {
		set("roomId", roomId);
		return (M)this;
	}

	public Integer getRoomId() {
		return getInt("roomId");
	}

	public M setApplyUserId(Integer applyUserId) {
		set("applyUserId", applyUserId);
		return (M)this;
	}

	public Integer getApplyUserId() {
		return getInt("applyUserId");
	}

	public M setSoleCode(String soleCode) {
		set("soleCode", soleCode);
		return (M)this;
	}

	public String getSoleCode() {
		return getStr("soleCode");
	}

	public M setUserName(String userName) {
		set("userName", userName);
		return (M)this;
	}

	public String getUserName() {
		return getStr("userName");
	}

	public M setApplyDate(String applyDate) {
		set("applyDate", applyDate);
		return (M)this;
	}

	public String getApplyDate() {
		return getStr("applyDate");
	}

	public M setApplyStartTime(String applyStartTime) {
		set("applyStartTime", applyStartTime);
		return (M)this;
	}

	public String getApplyStartTime() {
		return getStr("applyStartTime");
	}

	public M setApplyEndTime(String applyEndTime) {
		set("applyEndTime", applyEndTime);
		return (M)this;
	}

	public String getApplyEndTime() {
		return getStr("applyEndTime");
	}

	public M setRecordStatus(Integer recordStatus) {
		set("recordStatus", recordStatus);
		return (M)this;
	}

	public Integer getRecordStatus() {
		return getInt("recordStatus");
	}

	public M setRoomAddr(String roomAddr) {
		set("roomAddr", roomAddr);
		return (M)this;
	}

	public String getRoomAddr() {
		return getStr("roomAddr");
	}

	public M setRoomOpenTime(String roomOpenTime) {
		set("roomOpenTime", roomOpenTime);
		return (M)this;
	}

	public String getRoomOpenTime() {
		return getStr("roomOpenTime");
	}

	public M setRoomCloseTime(String roomCloseTime) {
		set("roomCloseTime", roomCloseTime);
		return (M)this;
	}

	public String getRoomCloseTime() {
		return getStr("roomCloseTime");
	}

	public M setRoomStatus(String roomStatus) {
		set("roomStatus", roomStatus);
		return (M)this;
	}

	public String getRoomStatus() {
		return getStr("roomStatus");
	}

	public M setRoomMode(String roomMode) {
		set("roomMode", roomMode);
		return (M)this;
	}

	public String getRoomMode() {
		return getStr("roomMode");
	}

	public M setIdNo(String idNo) {
		set("idNo", idNo);
		return (M)this;
	}

	public String getIdNo() {
		return getStr("idNo");
	}

	public M setIsSued(String isSued) {
		set("isSued", isSued);
		return (M)this;
	}

	public String getIsSued() {
		return getStr("isSued");
	}

	public M setDelFlag(String delFlag) {
		set("delFlag", delFlag);
		return (M)this;
	}

	public String getDelFlag() {
		return getStr("delFlag");
	}

	public M setIsFlag(String isFlag) {
		set("isFlag", isFlag);
		return (M)this;
	}

	public String getIsFlag() {
		return getStr("isFlag");
	}

}
