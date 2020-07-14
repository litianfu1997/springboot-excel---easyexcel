package com.sugon.excel;

import com.alibaba.excel.EasyExcel;
import com.sugon.excel.datalistener.DataListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExcelApplicationTests {

    /**
     * 不创建对象的读
     */
    @Test
    public void noModelRead() {
        String fileName = "C:\\Users\\Blunt\\Desktop\\exceltest\\test1.xlsx";
        // 这里 只要，然后读取第一个sheet 同步读取会自动finish
        EasyExcel.read(fileName, new DataListener()).sheet().doRead();
    }

    @Test
    void contextLoads() {
    }

}
