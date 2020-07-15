package com.sugon.excel.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;


/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/6 15:29
 * @email 1035869369@qq.com
 * 动态编译器
 */
public class CompilerJob {

    private static final Logger logger = LoggerFactory.getLogger(CompilerJob.class);

    /**
     * 将新生成的实体类进行动态编译
     */
    public void compiler(String fileName) {
        String name = fileName.split("\\.")[0];
        try {
            System.out.println(System.getProperty("user.dir"));
            //动态编译
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            int status = javac.run(null, null, null, "-d", System.getProperty("user.dir") + "\\target\\classes"
                    , System.getProperty("user.dir")+"\\src\\main\\java\\com\\sugon\\excel\\entity"+"\\ExcelEntity"+name+".java");
            if (status != 0) {
                logger.info("没有编译成功！");
            }else {
                logger.info("编译成功");

            }

        } catch (Exception e) {
            logger.error("编译异常", e);
        }
    }
}