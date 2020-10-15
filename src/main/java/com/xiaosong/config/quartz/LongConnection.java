package com.xiaosong.config.quartz;

import com.xiaosong.config.SendAccessRecord;
import com.xiaosong.model.TbDevice;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * 定时长连接
 */
public class LongConnection implements Job{
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            System.out.println("设备开始报警....");
            connection();
            Thread.sleep(300000);
            //Thread.sleep(299999);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connection() {
        SendAccessRecord accessRecord = new SendAccessRecord();
        List<TbDevice> devices= TbDevice.dao.find("select * from tb_device where status = '0'");
        for (TbDevice device : devices) {
            if(device.getDeviceType().equals("DS-K5671")){
                accessRecord.length();
            }
        }

    }
}
