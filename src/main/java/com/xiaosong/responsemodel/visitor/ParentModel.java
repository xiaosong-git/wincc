package com.xiaosong.responsemodel.visitor;

public class ParentModel {

    /*
     *
     * @verify 结果
     * @data   数据
     *
     */
    private VerifyModel verify;

    private DataModel data;

    public VerifyModel getVerify() {
        return verify;
    }

    public void setVerify(VerifyModel verify) {
        this.verify = verify;
    }

    public DataModel getData() {
        return data;
    }

    public void setData(DataModel data) {
        this.data = data;
    }
}
