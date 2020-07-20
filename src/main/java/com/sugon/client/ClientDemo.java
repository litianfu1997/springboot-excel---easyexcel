package com.sugon.client;

import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.util.ApiParam;
import com.sugon.excel.ws.ExcelServerWebService;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/15 9:49
 * @email 1035869369@qq.com
 * 代理工厂模式
 */
public class ClientDemo {
    public static void main(String[] args) {
        try {
            // 接口地址
            String address = "http://localhost:8080/services/api?wsdl";
            // 代理工厂
            JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
            // 设置代理地址
            jaxWsProxyFactoryBean.setAddress(address);
            // 设置接口类型
            jaxWsProxyFactoryBean.setServiceClass(ExcelServerWebService.class);
            // 创建一个代理接口实现
            ExcelServerWebService excelServerWebService = (ExcelServerWebService) jaxWsProxyFactoryBean.create();

            String excelName = "test1.xlsx";

            ApiParam apiParam = new ApiParam();
            apiParam.setPage(1);
            apiParam.setLimit(20);
            apiParam.setSelect("*");
            apiParam.setWhere(null);
            apiParam.setOrder(null);
            // 调用代理接口的方法调用并返回结果
            ResultEntity result = excelServerWebService.selectExcel(excelName, apiParam);
            System.out.println("返回结果:" + result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

