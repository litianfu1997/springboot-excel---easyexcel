package com.sugon.excel.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;

import com.sugon.excel.datalistener.DataListener;
import com.sugon.excel.exception.GlobalException;
import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import com.sugon.excel.service.ExcelServerProviderService;
import com.sugon.excel.util.ApiOrder;
import com.sugon.excel.util.ApiParam;
import com.sugon.excel.util.ApiWhere;
import com.sugon.excel.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/3 17:20
 * @email 1035869369@qq.com
 * 该类是对excel文件进行读取并提供对应api对excel文件进行查询
 */
@RestController
@RequestMapping("/excelServer")
@Scope("prototype")
public class ExcelServerProviderController {

    private final static Logger logger = LoggerFactory.getLogger(ExcelServerProviderController.class);

    /**
     * 请求记录，用于gc
     */
    private static Integer requestCount = 0;

    /**
     * redis工具
     */
    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ExcelServerProviderService excelServerProviderService;


    /**
     * 存放excel表格的路径
     */
    @Value("${excel.path}")
    private String filepath;


    /**
     * 类sql查询excel
     *
     * @param objectMap
     * @return
     */
    @RequestMapping("/{excelName}/selectExcel")
    public ResultEntity selectExcel(@PathVariable("excelName") String excelName, @RequestBody ApiParam apiParam) {

        //表格名
        String sheet = excelName;
        //查询字段
        String select = (String) apiParam.getSelect();
        //页码
        Integer page = (Integer) apiParam.getPage();
        //页面数据条数
        Integer limit = (Integer) apiParam.getLimit();
        //查询条件
        List<ApiWhere> where = apiParam.getWhere();
        //排序字段
        List<ApiOrder> order = apiParam.getOrder();
        if (where.size()!=0){
            where.stream().forEach(o->{
                if (StringUtils.isEmpty(o.getKey())
                        || StringUtils.isEmpty(o.getOperator())
                        ||StringUtils.isEmpty(o.getValue())) {
                    throw new GlobalException("检索条件不能为空");
                }
            });
        }
        if (order.size()!=0){
            order.stream().forEach(o->{
                if (StringUtils.isEmpty(o.getKey())
                        || StringUtils.isEmpty(o.getBy())) {
                    throw new GlobalException("排序键或者排序方式为空");
                }
            });
        }
        if (page == null || page == 0) {
            page = 1;
        }

        if (limit == null || page == 0) {
            limit = 20;
        }

        if (sheet == null) {
            throw  new GlobalException("该表不存在");
        }

        List<Map<String, String>> result = excelServerProviderService
                .getExcelData(sheet, select, page, limit, where, order);

        return new ResultEntity(ResultEnum.SUCCESS, result);
    }


    /**
     * 删除本地excel文件,与缓存
     *不能删除远程文件
     * @param excelname excel文件名
     * @return
     */
    @RequestMapping("removeExcelFileByName")
    public ResultEntity removeExcelFileByName(String excelname) {

        //excel路径
        String fileName = filepath + excelname;

        File file = new File(fileName);

        if (file.exists()) {
            boolean delete = file.delete();
            if (delete == true) {
                this.removeRedisCacheByName(excelname);
                return new ResultEntity(ResultEnum.SUCCESS);
            } else {
                throw  new GlobalException("删除失败");
            }
        }

        return new ResultEntity(0, "SYS_EOR", "删除失败，文件不存在");
    }

    /**
     * 移除redis缓存的数据
     * 请传输完整文件名，例如"test1.xlsx"
     *
     * @param excelname excel文件名
     * @return
     */
    @RequestMapping("removeRedisCacheByName")
    public ResultEntity removeRedisCacheByName(String excelname) {

        //删除redis对应表数据
        Long del = redisUtils.del("full-" + excelname);
        if (del > 0) {
            return new ResultEntity(ResultEnum.SUCCESS);
        }

        return new ResultEntity(0, "SYS_EOR", "删除失败");
    }


}

