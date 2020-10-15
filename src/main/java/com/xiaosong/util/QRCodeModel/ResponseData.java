package com.xiaosong.util.QRCodeModel;

import com.xiaosong.model.TbCompanyuser;

import java.util.List;

public class ResponseData {

	private int pageNum;
	private String pospCode;
	private String orgCode;

	public String getPospCode() {
		return pospCode;
	}

	public void setPospCode(String pospCode) {
		this.pospCode = pospCode;
	}

	public String getOrgCode() {
		return orgCode;
	}

	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	private String mac;

	private int pageSize;
	
	private int totalPage;
	
	private int total;

	private List<TbCompanyuser> rows;

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<TbCompanyuser> getRows() {
		return rows;
	}

	public void setRows(List<TbCompanyuser> rows) {
		this.rows = rows;
	}


}
