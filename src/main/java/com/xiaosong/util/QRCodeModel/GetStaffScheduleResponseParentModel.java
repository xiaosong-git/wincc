package com.xiaosong.util.QRCodeModel;

public class GetStaffScheduleResponseParentModel {

	/*
	 * 
	 * @verify 结果
	 * @data   数据
	 * 
	 */
	private VerifyResponseModel verify;
	private GetStaffScheduleDataResponseModel data;

	public VerifyResponseModel getVerify() {
		return verify;
	}

	public void setVerify(VerifyResponseModel verify) {
		this.verify = verify;
	}

	public GetStaffScheduleDataResponseModel getData() {
		return data;
	}

	public void setData(GetStaffScheduleDataResponseModel data) {
		this.data = data;
	}
}
