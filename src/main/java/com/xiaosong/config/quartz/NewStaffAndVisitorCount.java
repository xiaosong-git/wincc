package com.xiaosong.config.quartz;

import com.xiaosong.common.server.ServerService;
import com.xiaosong.common.wincc.visitor.VisService;
import com.xiaosong.common.wincc.companyuser.StaffService;
import com.xiaosong.model.TbBuildingServer;
import com.xiaosong.model.TbStatement;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  当天 新增员工和访客数量报表
 */
public class NewStaffAndVisitorCount implements Job {
    private ServerService srvServer = ServerService.me; //服务器业务层

    private VisService srvVisitor = VisService.me;

    private StaffService srvStaff = StaffService.me;

    private static Logger logger = Logger.getLogger(NewStaffAndVisitorCount.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            TbBuildingServer tbServerinfo = srvServer.findSer();
            if (tbServerinfo.getOrgCode().isEmpty()) {
                logger.error("大楼编号不存在.");
                return;
            }
            //当天新增员工数量
            Integer staffNowCount = srvStaff.findByStaffNowCount();

            //当天新增访客数量
            Integer visitorNowCount = srvVisitor.findByVisitorNowCount();

            //当前上位机总员工数
            Integer staffCount = srvStaff.findByStaffCount();

            //当日下发人数 员工
            Integer sendStaffSuccessCount = srvStaff.findByStaffNowSuccessCount();
            //当日下发人数 访客
            Integer sendVisitorSuccessCount = srvVisitor.findByVisitorNowSuccessCount();
            //当日下发人数
            Integer nowSendSuccessCount = sendStaffSuccessCount+sendVisitorSuccessCount;

            //当日下发人数 员工
            Integer sendStaffFailCount = srvStaff.findByStaffNowFailCount();
            //当日下发人数 访客
            Integer sendVisitorFailCount = srvVisitor.findByVisitorNowFailCount();
            //当日下发失败人数
            Integer nowSendFailCount = sendStaffFailCount+sendVisitorFailCount;

            //当日删除人数
            Integer StaffNowDelCount = srvStaff.findByStaffDelCount();



            TbStatement tbStatement = new TbStatement();
            tbStatement.setStaffNowCount(visitorNowCount);
            tbStatement.setVisitorNowCount(staffNowCount);
            tbStatement.setStaffCount(staffCount);
            tbStatement.setNowSendSuccessCount(nowSendSuccessCount);
            tbStatement.setNowSendFailCount(nowSendFailCount);
            tbStatement.setStaffNowDelCount(StaffNowDelCount);
            tbStatement.setNewDate(getDate());
            tbStatement.save();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取 当前时间的 年-月-日
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        return df.format(new Date()); // new Date()为获取当前系统时间
    }
}