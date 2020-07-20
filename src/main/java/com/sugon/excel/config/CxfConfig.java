package com.sugon.excel.config;



import com.sugon.excel.ws.ExcelServerWebService;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;
import java.util.ArrayList;


/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/2 11:05
 * @email 1035869369@qq.com
 */
@Configuration
public class CxfConfig {

    @Autowired
    private ExcelServerWebService excelServerWebService;


    @Autowired(required = false)
    private Bus bus;

    @Bean
    public ServletRegistrationBean newServlet() {
        return new ServletRegistrationBean(new CXFServlet(), "/services/*");
    }


    /**
     * 构造一个json转化bean，用于将实体类转化为json
     *
     * @return
     */
    @Bean
    public JacksonJsonProvider getJacksonJsonProvider() {
        return new JacksonJsonProvider();

    }

    @Bean
    public Endpoint endpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, excelServerWebService);
        endpoint.publish("/api");
        return endpoint;
    }


    /**
     * 配置restful接口
     */
    @Bean
    public Server server() {
        //添加接口
        ArrayList<Object> services = new ArrayList<>();
        //多个接口，请依次添加
        services.add(excelServerWebService);

        JAXRSServerFactoryBean bean = new JAXRSServerFactoryBean();
        bean.setBus(bus);
        bean.setAddress("/");
        bean.setProvider(getJacksonJsonProvider());
        bean.setServiceBeans(services);
        return bean.create();
    }


}
