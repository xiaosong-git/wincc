package com.xiaosong.util;

import com.xiaosong.model.TbVisitor;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckUtils {

	private static Logger logger = Logger.getLogger(CheckUtils.class);
	/*
	 * 访客访问二维码验证
	 */
	public boolean verificationCache(TbVisitor visitor) throws ParseException {

		String nowtime = getDate();

		boolean period = compareDate(nowtime, visitor.getPreStartTime());
		boolean after = compareDate(visitor.getEndDateTime(), nowtime);

		if (period && after) {
			return true;
		} else {
			logger.error("访问时间不在预约时间内");
			return false;
		}
	}

	private String getDate() {
		// TODO Auto-generated method stub
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		String date = df.format(new Date()); // new Date()为获取当前系统时间
		return date;
	}

	private String getDate2() {
		// TODO Auto-generated method stub
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		String date = df.format(new Date()); // new Date()为获取当前系统时间
		return date;
	}

	private String getDate3() {
		// TODO Auto-generated method stub
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");// 设置日期格式
		String date = df.format(new Date()); // new Date()为获取当前系统时间
		return date;
	}

	private boolean compareDate(String DATE1, String DATE2) throws ParseException {

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		Date dt1 = df.parse(DATE1);
		Date dt2 = df.parse(DATE2);

		if (dt1.getTime() > dt2.getTime()) {
			return true;
		} else if (dt1.getTime() < dt2.getTime()) {
			return false;
		} else {
			return true;
		}

	}
}
