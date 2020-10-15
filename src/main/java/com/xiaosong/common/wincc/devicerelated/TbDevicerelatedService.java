package com.xiaosong.common.wincc.devicerelated;


import com.xiaosong.common.wincc.device.TbDeviceService;
import com.xiaosong.model.TbDevice;
import com.xiaosong.model.TbDevicerelated;

import java.util.ArrayList;
import java.util.List;

public class TbDevicerelatedService {
    public static final TbDevicerelatedService me = new TbDevicerelatedService();

    public static final TbDeviceService deviceService = TbDeviceService.me;

    static final TbDevicerelated dao = TbDevicerelated.dao;

    public TbDevicerelated findByFaceIP(String deviceIp){
        return dao.findFirst("select * from tb_devicerelated where faceIP = ?",deviceIp);
    }

    public List<TbDevicerelated> findFIPbyFloor(String companyFloor){
        return dao.find("select * from tb_devicerelated where contralFloor LIKE CONCAT('%|',?,'|%') and faceIP !=''",companyFloor);
    }

    public List<String> getAllFaceDeviceIP(String companyFloor) {
        List<String> allFaceIP = new ArrayList<String>();
        //楼层为空，下发全部设备
        if (companyFloor ==null||companyFloor==""||"undefined".equals(companyFloor)||companyFloor.length()==0) {
            List<TbDevicerelated> list = dao.findAll();
            if(list.size() > 0){
                for (int i = 0; i < list.size(); i++) {
                    String faceIp = list.get(i).getFaceIP();
                    allFaceIP.add(faceIp);
                }
            }
        }else{
            //对应多楼层设备
            if (companyFloor.contains("|")) {
                String[] floors = companyFloor.split("\\|");
                for (String floor : floors) {
                    List<TbDevicerelated> list = findFIPbyFloor(floor);
                    for (int i = 0; i < list.size(); i++) {
                        String faceIp =  list.get(i).getFaceIP();
                        // 排查重复 当设备包含大楼N次时，只取一次
                        TbDevice device =deviceService.findByDeviceIp(faceIp);
                        if (!allFaceIP.contains(faceIp)) {
                            allFaceIP.add(faceIp);
                        }
                    }
                }
            }else{
                List<TbDevicerelated> list = findFIPbyFloor(companyFloor);
                for (int i = 0; i < list.size(); i++) {
                    String faceIp =  list.get(i).getFaceIP();
                    // 排查重复 当设备包含大楼N次时，只取一次
                    TbDevice device =deviceService.findByDeviceIp(faceIp);
                    if (!allFaceIP.contains(faceIp)) {
                        allFaceIP.add(faceIp);
                    }
                }
            }
        }
        return allFaceIP;
    }
}
