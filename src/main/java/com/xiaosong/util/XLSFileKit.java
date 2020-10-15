package com.xiaosong.util;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class XLSFileKit {
    // 创建一个`excel`文件
    private HSSFWorkbook workBook;

    // `excel`文件保存路径
    private String filePath;

    public XLSFileKit(String filePath) {
        this.filePath = filePath;
        this.workBook = new HSSFWorkbook();
    }

    /**
     * 添加sheet
     *
     * @param content   数据
     * @param sheetName sheet名称
     * @param title     标题
     */
    public <T> void addSheet(List<List<T>> content, String sheetName, List<String> title) {
        HSSFSheet sheet = this.workBook.createSheet(sheetName);
        Font font = this.workBook.createFont();
        //字体大小
        font.setFontHeightInPoints((short) 12);
        //设置字体
        font.setFontName("楷体");
        //字体颜色
        //font.setColor(Font.COLOR_RED);

        //6.2创建单元格格式CellStyle
        CellStyle cellStyle = this.workBook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // `excel`中的一行
        HSSFRow row = null;

        // `excel`中的一个单元格
        HSSFCell cell = null;
        int i = 0, j = 0;

        // 创建第一行，添加`title`
        row = sheet.createRow(0);
        for (; j < title.size(); j++) {//添加标题
            cell = row.createCell(j);
            cell.setCellValue(title.get(j));
            cell.setCellStyle(cellStyle);
        }
        // 必须在单元格设值以后进行
        // 设置为根据内容自动调整列宽
        for (int k = 1; k < title.size(); k++) {
            sheet.autoSizeColumn(4000);
            // 处理中文不能自动调整列宽的问题
            sheet.setColumnWidth(k, sheet.getColumnWidth(i) * 20 / 10);
        }

        // 创建余下所有行
        i = 1;
        for (List<T> rowContent : content) {
            row = sheet.createRow(i);
            j = 0;
            for (Object cellContent : rowContent) {
                cell = row.createCell(j);
                cell.setCellValue(cellContent.toString());
                cell.setCellStyle(cellStyle);
                j++;
            }
            i++;
        }
        //显示的 行数
        row = sheet.createRow(content.size() + 1);
        //显示的 列
        cell = row.createCell(8);
        //显示 的 文字
        cell.setCellValue("总记录条数: " + content.size());
        cell.setCellStyle(cellStyle);
    }

    /**
     * 保存
     *
     * @return
     */
    public boolean save() {
        try {
            FileOutputStream fos = new FileOutputStream(this.filePath);
            this.workBook.write(fos);
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
