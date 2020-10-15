package com.xiaosong.responsemodel.companyusers;

import java.util.List;

public class DataModel {
    private int pageNum;

    private int pageSize;

    private int totalPage;

    private int total;

    private List<CompanyUserList> rows;

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

    public List<CompanyUserList> getRows() {
        return rows;
    }

    public void setRows(List<CompanyUserList> rows) {
        this.rows = rows;
    }
}
