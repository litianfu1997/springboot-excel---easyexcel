package com.sugon.excel.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.sugon.excel.annotation.SaveToCache;
import com.sugon.excel.datalistener.DataListener;
import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import com.sugon.excel.util.ChineseToSpell;
import com.sugon.excel.util.RedisUtils;
import jdk.nashorn.internal.ir.IfNode;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
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

    private static Integer requestCount = 0;

    @Autowired
    private RedisUtils redisUtils;


    /**
     * 存放excel表格的路径
     */
    @Value("${excel.path}")
    private String filepath;

    /**
     * 数据监听器
     */
    private DataListener dataListener;


    /**
     * 读取excel表格，并存入redis缓存中
     * @return
     */
    @RequestMapping(value = "/test")
    public ResultEntity selectRowByAnyKey(@RequestBody Map<String, Object> objectMap) {

        String sheet = objectMap.get("sheet").toString();
        //excel路径
        String fileName = filepath + sheet;
        //表头数据
        Map<Integer, String> headMap = new HashMap<>();
        //表的数据
        List<Map<Integer, String>> list = null;

        //如果redis里面没有这个表格的缓存数据，读表，存redis
        if (redisUtils.get(sheet) == null && redisUtils.get(sheet + "-head") == null) {
            // 这里 只要，然后读取第一个sheet 同步读取会自动finish
            dataListener = new DataListener();
            //读取excel
            EasyExcel.read(fileName, dataListener).sheet().doRead();
            //获取表头数据
            headMap = dataListener.getHeadMap();
            //获取表的数据
            list = dataListener.getList();

            //将表头数据存入redis
            redisUtils.set(sheet + "-head", JSON.toJSONString(headMap));

            //将表数据存入redis
            redisUtils.set(sheet, JSON.toJSONString(list));

        } else {
            //redis缓存有数据
            //获取表数据
            String dataJsonString = redisUtils.get(sheet);
            //获取表头数据
            String headMapJsonString = redisUtils.get(sheet + "-head");
            //json字符串转换
            list = (List<Map<Integer, String>>) JSON.parseArray(dataJsonString,headMap.getClass());
            headMap = (Map<Integer, String>) JSON.parseObject(headMapJsonString,Map.class);
        }


        //用户传过来的map
        Map<String, String> data = (Map<String, String>) objectMap.get("data");

        int notNullCount = 0;

        //遍历前端传递过来的entityMap
        Iterator<Map.Entry<String, String>> entries = data.entrySet().iterator();
        while (entries.hasNext()) {

            Map.Entry<String, String> entry = entries.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            //如果传入的key不存在
            if (!headMap.containsValue(key)) {
                return new ResultEntity(0, "SYS_ERROR", "key不存在！");
            }

            if (value == null) {
                continue;
            }
            //计算entityMap不为空的元素
            notNullCount++;
        }


        //记录不为空的字段
        int finalNotNullCount = notNullCount;

        Map<Integer, String> finalHeadMap = headMap;
        //过滤list
        List<Map<Integer, String>> result = list.stream().filter(e -> {
            Iterator<Map.Entry<Integer, String>> keyEntries = finalHeadMap.entrySet().iterator();
            List<Boolean> booleanList = new ArrayList<>();
            while (keyEntries.hasNext()) {
                Map.Entry<Integer, String> entry = keyEntries.next();
                Integer key = entry.getKey();
                String value = entry.getValue();
                //如果遇到空的格子就跳过
                if (e.get(key) == null) {
                    continue;
                }
                //查询符合条件的行
                if (e.get(key).equals(data.get(value))) {
                    booleanList.add(true);
                }
            }
            //判断标志数量是否等于传参个数
            return booleanList.size() == finalNotNullCount ? true : false;
        }).collect(Collectors.toList());

        List<Map<String, String>> resultList = this.assembleMap(headMap, result);

        //每隔100次请求gc一次
        if (++requestCount == 100) {
            requestCount = 0;
            System.gc();
            logger.info("System.gc();");
        }


        return new ResultEntity(ResultEnum.SUCCESS, resultList);
    }


    /**
     * 组装map
     *
     * @return
     */
    public List<Map<String, String>> assembleMap(Map<Integer, String> head, List<Map<Integer, String>> datalist) {

        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> newMap = null;

        for (Map<Integer, String> oldMap : datalist) {
            newMap = new HashMap<>();
            Iterator<Map.Entry<Integer, String>> keyEntries = head.entrySet().iterator();
            while (keyEntries.hasNext()) {
                Map.Entry<Integer, String> entry = keyEntries.next();
                Integer key = entry.getKey();
                String value = entry.getValue();
                newMap.put(value, oldMap.get(key));
            }
            list.add(newMap);
        }

        return list;
    }


}

