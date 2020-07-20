package com.sugon.excel.ws;

import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.util.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/20 10:01
 * @email 1035869369@qq.com
 */
@WebService
@Consumes({MediaType.APPLICATION_XML}) // 请求类型
public interface ExcelServerWebService {


    /**
     * 测试获取信息
     * Produces 返回值类型
     * @param info
     * @return
     */
    @Path("/getInfo")
    @Produces({MediaType.APPLICATION_XML})
    String testInfo(@WebParam(name = "info") String info);


    /**
     * 类sql查询excel
     *
     * @param apiParam
     * @param excelName
     * @return
     */
    @Path("/selectExcel")
    @Produces({MediaType.APPLICATION_XML})
    ResultEntity selectExcel(@WebParam(name = "excelName") String excelName, @WebParam(name = "apiParam") ApiParam apiParam);


    /**
     * 删除本地excel文件,与缓存
     * 不能删除远程文件
     *
     * @param excelName excel文件名
     * @return
     */
    @Path("removeExcelFileByName")
    ResultEntity removeExcelFileByName(@WebParam(name = "excelName") String excelName);


    /**
     * 移除redis缓存的数据
     * 请传输完整文件名，例如"test1.xlsx"
     *
     * @param excelName excel文件名
     * @return
     */
    @Path("removeRedisCacheByName")
    ResultEntity removeRedisCacheByName(@WebParam(name = "excelName") String excelName);
}
