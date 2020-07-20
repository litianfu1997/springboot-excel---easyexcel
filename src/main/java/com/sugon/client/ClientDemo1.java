package com.sugon.client;

import com.sugon.excel.util.ApiParam;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.util.ArrayList;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/15 10:02
 * @email 1035869369@qq.com
 * 动态工厂模式
 */
public class ClientDemo1 {
    public static void main(String[] args) {
        // 创建动态客户端
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://localhost:8080/services/api?wsdl");
        // 需要密码的情况需要加上用户名和密码
        // client.getOutInterceptors().add(new ClientLoginInterceptor(USER_NAME, PASS_WORD));
        Object[] objects = new Object[0];
        try {
            String excelName = "test1.xlsx";

            ApiParam apiParam = new ApiParam();
            apiParam.setPage(1);
            apiParam.setLimit(20);
            apiParam.setSelect("*");
            apiParam.setWhere(new ArrayList<>());
            apiParam.setOrder(new ArrayList<>());


            // invoke("方法名",参数1,参数2,参数3....);
            objects = client.invoke("selectExcel", excelName, apiParam);
            System.out.println("返回数据:" + objects);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
