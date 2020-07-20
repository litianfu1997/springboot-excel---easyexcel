package com.sugon.excel.ws;

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.sugon.excel.controller.ExcelServerProviderController;
import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import com.sugon.excel.util.ApiParam;
import com.sugon.excel.ws.ExcelServerWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/20 10:10
 * @email 1035869369@qq.com
 */
@Service
public class ExcelServerWebServiceImpl implements ExcelServerWebService {

    @Autowired
    private ExcelServerProviderController excelServerProviderController;

    /**
     * 测试连通性
     * @param info
     * @return
     */
    @Override
    public String testInfo(String info) {
        return "返回的信息:" + info;
    }

    /**
     * 类sql查询Excel
     * @param excelName
     * @param apiParam
     * @return
     */
    @Override
    public ResultEntity selectExcel(String excelName, ApiParam apiParam) {
        System.out.println(apiParam);
        if (apiParam.getWhere() == null || StringUtils.isEmpty(apiParam.getWhere().get(0).getKey())) {
            apiParam.setWhere(new ArrayList<>());
        }
        if (apiParam.getOrder() == null || StringUtils.isEmpty(apiParam.getOrder().get(0).getKey())) {
            apiParam.setOrder(new ArrayList<>());
        }

        ResultEntity resultEntity = excelServerProviderController.selectExcel(excelName, apiParam);
        return new ResultEntity(resultEntity.getStatus(),resultEntity.getCode()
                ,resultEntity.getMessage(),JSON.toJSONString(resultEntity.getData()));

    }

    /**
     * 删除文件
     * @param excelName excel文件名
     * @return
     */
    @Override
    public ResultEntity removeExcelFileByName(String excelName) {
        ResultEntity resultEntity = excelServerProviderController.removeExcelFileByName(excelName);
        return new ResultEntity(resultEntity.getStatus(),resultEntity.getCode(),resultEntity.getMessage());
    }

    /**
     * 删除缓存
     * @param excelName excel文件名
     * @return
     */
    @Override
    public ResultEntity removeRedisCacheByName(String excelName) {
        ResultEntity resultEntity = excelServerProviderController.removeRedisCacheByName(excelName);
        return new ResultEntity(resultEntity.getStatus(),resultEntity.getCode(),resultEntity.getMessage());
    }
}
