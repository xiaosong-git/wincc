package com.xiaosong.responsemodel.visitor;

import java.util.List;

public class DataModel {

    private int pageNum;
    private int pageSize;
    private String totalPage;
    private String total;
    private List<VisitorListModel> rows;

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

    public String getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(String totalPage) {
        this.totalPage = totalPage;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public List<VisitorListModel> getRows() {
        return rows;
    }

    public void setRows(List<VisitorListModel> rows) {
        this.rows = rows;
    }
}
