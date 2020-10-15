package com.xiaosong.common.wincc.failreceive;

import com.xiaosong.model.TbFailreceive;

import java.util.List;

public class FailReceService {
    public static FailReceService me = new FailReceService();

    /**
     *  根据设备ip  访客姓名 ,访客身份证 , 人员类型 查询下发失败的数据
     * @param deviceIp      设备ip
     * @param visitorName   访客姓名
     * @param visitorIdCard 访客身份证号码
     * @param visitor       人员类型（员工-staff，访客-visitor,发起共享-applicat）
     * @return
     */
    public TbFailreceive findOne(String deviceIp, String visitorName, String visitorIdCard, String visitor) {
        return TbFailreceive.dao.findFirst("select * from tb_failreceive where faceIp = ? and userName = ? and idCard = ? and userType = ?",
                deviceIp, visitorName, visitorIdCard, visitor);
    }

    /**
     * 根据 用户名 ,失败操作 ,下发次数 查询发下失败数据
     * @param userName 用户名
     * @param opera    失败操作（save-下发，delete-删除）（主键4）
     * @return
     */
    public List<TbFailreceive> findByName(String userName, String opera) {
//        return TbFailreceive.dao.find("select * from tb_failreceive where userName = ? and receiveFlag = '1' and opera = ? and downNum<5", userName, opera);
        return TbFailreceive.dao.find("select * from tb_failreceive where userName = ? and receiveFlag = '1' and opera = ? and downNum<5", userName, opera);

    }

    /**
     * 根据 用户名  身份证 人员类型 查询 下发失败的 数据
     * @param userName  用户名
     * @param idNO      身份证
     * @param staff     人员类型（员工-staff，访客-visitor,发起共享-applicat）
     * @return
     */
    public List<TbFailreceive> findByNameAndid(String userName, String idNO, String staff) {
//        return TbFailreceive.dao.find("select * from tb_failreceive where userName = ? and idCard = ? and userType = ? and downNum<5", userName, idNO, staff);
        return TbFailreceive.dao.find("select * from tb_failreceive where userName = ? and idCard = ? and userType = ? and downNum<5", userName, idNO, staff);
    }

    /**
     * 根据 访客id 查询下发失败的数据
     * @param id 访客id
     * @return
     */
    public TbFailreceive findByVisitorUUId(String id) {
        return TbFailreceive.dao.findFirst("select * from tb_failreceive where failedId = ?", id);
    }

    /**
     * 根据下发接收标识  人员类型  查询下发失败的数据
     * @param receiveFlag  下发接收标识 （0-成功，1-失败）
     * @param uerType       人员类型（员工-staff，访客-visitor,发起共享-applicat）
     * @return
     */
    public List<TbFailreceive> findByFaceFlag(String receiveFlag, String uerType) {
        return TbFailreceive.dao.find("select * from tb_failreceive where receiveFlag = ? and userType = ?", receiveFlag, uerType);
    }

    /**
     * 根据 访客姓名 ,访客身份证 ,人员类型 ,下发次数 查询 下发失败的数据
     * @param visitorName       访客姓名
     * @param visitorIdCard     访客身份证
     * @param visitor           人员类型（员工-staff，访客-visitor,发起共享-applicat）
     * @return
     */
    public List<TbFailreceive> findFaceIP(String visitorName, String visitorIdCard, String visitor) {
        return TbFailreceive.dao.find("select * from tb_failreceive where userName = ? and idCard = ? and userType =? and receiveFlag='1' and opera ='deleted'",visitorName,visitorIdCard,visitor);
    }
}
