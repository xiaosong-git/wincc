package com.xiaosong.common.wincc.failreceive;


import com.xiaosong.model.TbFailreceive;

import java.util.List;

public class FailReceiveService {
    public static final FailReceiveService me = new FailReceiveService();

    public static final TbFailreceive failreceive = TbFailreceive.dao;

    public TbFailreceive findOne(String faceIp,String userName,String idNO,String userType){
        return failreceive.findFirst("select * from tb_failreceive where faceIp = ? and userName = ? and idCard = ? and userType = ?",faceIp,userName,idNO,userType);
    }

    public List<TbFailreceive> findByFaceFlag(String receiveFlag,String userType){
        return failreceive.find("select * from tb_failreceive where receiveFlag = ? and userType = ? ",receiveFlag,userType);
    }

    public List<TbFailreceive> findByName(String userName ,String opera){
        return failreceive.find("select * from tb_failreceive where userName = ? and opera = ? and receiveFlag = '1' and downNum <5",userName,opera);
    }

}
