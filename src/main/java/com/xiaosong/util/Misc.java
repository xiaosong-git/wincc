package com.xiaosong.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Misc {

	
	/**
	 * 比较日期（年月日时分）
	 * 
	 * @param DATE1		yyyy-MM-dd HH:mm
	 * @param DATE2		yyyy-MM-dd HH:mm
	 * @return
	 * @throws ParseException
	 */
	 public static boolean compareDate(String DATE1, String DATE2) throws ParseException {
		 
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
	 /**
	  * 
	  * 比较日期（年月日）
	  * 
	  * @param DATE1	yyyy-MM-dd
	  * @param DATE2	yyyy-MM-dd
	  * @return
	  * @throws ParseException
	  */
	 public static boolean compareDate2(String DATE1, String DATE2) throws ParseException {
		 DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		 
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
	 /**
	  *
	  * 比较日期（年月日）
	  *
	  * @param DATE1	yyyy-MM-dd
	  * @param DATE2	yyyy-MM-dd
	  * @return
	  * @throws ParseException
	  */
	 public static boolean compareData3(String DATE1, String DATE2) throws ParseException {
		 DateFormat df = new SimpleDateFormat("HH:mm:ss");

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
