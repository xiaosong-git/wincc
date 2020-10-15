package com.xiaosong.util.QRCodeModel;


import com.xiaosong.model.TbCompanyuser;

import java.util.List;

public class GetCompanyUserScheduleModel {

	private VerifyResponseModel verify;
	private List<TbCompanyuser> data;
	public VerifyResponseModel getVerify() {
		return verify;
	}
	public void setVerify(VerifyResponseModel verify) {
		this.verify = verify;
	}
	public List<TbCompanyuser> getData() {
		return data;
	}
	public void setData(List<TbCompanyuser> data) {
		this.data = data;
	}
	@Override
	public String toString() {
		return "GetCompanyUserScheduleModel [verify=" + verify + ", data=" + data + "]";
	}

	
}
