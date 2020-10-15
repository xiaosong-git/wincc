package com.xiaosong.responsemodel.companyusers;

import java.util.List;

public class ParentModel {

    private VerifyModel verify;
    private List<CompanyUserList> data;

    public VerifyModel getVerify() {
        return verify;
    }

    public void setVerify(VerifyModel verify) {
        this.verify = verify;
    }

    public List<CompanyUserList> getData() {
        return data;
    }

    public void setData(List<CompanyUserList> data) {
        this.data = data;
    }
}
