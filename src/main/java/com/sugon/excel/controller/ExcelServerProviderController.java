package com.sugon.excel.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.sugon.excel.datalistener.DataListener;
import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import com.sugon.excel.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.io.File;
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


    /**
     * 存放excel表格的路径
     */
    @Value("${excel.path}")
    private String filepath;

    /**
     * 数据监听器
     */
    private DataListener dataListener;


    @RequestMapping("/selectExcel")
    public ResultEntity selectExcel(@RequestBody Map<String, Object> objectMap) {

        //表格名
        String sheet = (String) objectMap.get("sheet");
        //查询字段
        String select = (String) objectMap.get("select");
        //页码
        Integer page = (Integer) objectMap.get("page");
        //页面数据条数
        Integer limit = (Integer) objectMap.get("limit");
        //查询条件
        List<Map<String, Object>> where = (List<Map<String, Object>>) objectMap.get("where");
        //排序字段
        List<Map<String, Object>> order = (List<Map<String, Object>>) objectMap.get("order");
        if (page == null) {
            page = 0;
        }

        if (limit == null) {
            limit = 20;
        }

        if (sheet == null) {
            return new ResultEntity(0, "SYS_EOR", "该表不存在");
        }

        //读取表格数据，并将数据存入redis
        this.readExcel(objectMap);

        Map<String, String> fullMap = new HashMap<>();
        String s = redisUtils.get("full-" + sheet);
        List<Map<String, String>> mapList = (List<Map<String, String>>) JSON.parseArray(s, fullMap.getClass());

        List<Map<String, String>> result = mapList.stream()
                //过滤数据
                .filter(e -> {
                    List<AtomicBoolean> boolList = new ArrayList<>();
                    if (where.size() == 0) {
                        return true;
                    }
                    where.stream().forEach(o -> {
                        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                        String key = (String) o.get("key");
                        String operator = (String) o.get("operator");
                        Object value = o.get("value");
                        switch (operator) {
                            //等于
                            case "eq":
                                atomicBoolean.set(e.get(key).equals(value.toString()));
                                boolList.add(atomicBoolean);
                                break;
                            //小于
                            case "lt":
                                atomicBoolean.set(e.get(key).compareTo(value.toString()) < 0);
                                boolList.add(atomicBoolean);
                                break;
                            //大于
                            case "gt":
                                atomicBoolean.set(e.get(key).compareTo(value.toString()) > 0);
                                boolList.add(atomicBoolean);
                                break;
                            //小于等于
                            case "lte":
                                atomicBoolean.set(e.get(key).compareTo(value.toString()) <= 0);
                                boolList.add(atomicBoolean);
                                break;
                            //大于等于
                            case "gte":
                                atomicBoolean.set(e.get(key).compareTo(value.toString()) >= 0);
                                boolList.add(atomicBoolean);
                                break;
                            //模糊查询，包含字符串就行 %福%
                            case "lk":
                                atomicBoolean.set(e.get(key).indexOf(value.toString()) == -1 ? false : true);
                                boolList.add(atomicBoolean);
                                break;
                            //左边模糊 %福
                            case "ll":
                                atomicBoolean.set(e.get(key).indexOf(value.toString()) == -1 ? false : true
                                        && e.get(key).endsWith(value.toString()));
                                boolList.add(atomicBoolean);
                                break;
                            //右边模糊 李%
                            case "lr":
                                atomicBoolean.set(e.get(key).indexOf(value.toString()) == -1 ? false : true
                                        && e.get(key).startsWith(value.toString(),e.get(key).indexOf(value.toString())));
                                boolList.add(atomicBoolean);
                                break;

                            default:
                                atomicBoolean.set(false);
                                boolList.add(atomicBoolean);
                                break;

                        }

                    });
                    boolean b = boolList.stream().allMatch(c -> c.get() == true);

                    return b;
                })
                //排序
                .sorted((obj1, obj2) -> {
                    //如果排序字段为空，自然排序
                    if (order.size() == 0) {
                        return 1;
                    }
                    AtomicInteger flag = new AtomicInteger();
                    order.stream().forEach(o -> {
                        String key = (String) o.get("key");
                        String by = (String) o.get("by");
                        switch (by) {
                            case "asc":
                                flag.set(obj1.get(key).compareTo(obj2.get(key)) <= 0 ? -1 : 1);
                                break;
                            case "desc":
                                flag.set(obj1.get(key).compareTo(obj2.get(key)) >= 0 ? -1 : 1);
                                break;
                        }
                    });
                    return flag.get();
                })
                //字段映射
                .map(e -> {
                    String[] field;
                    if ("*".equals(select)||select == null){
                        Set<String> set = e.keySet();
                        field = set.toArray(new String[set.size()]);
                    }else {
                        field = select.split(",");
                    }
                    StringBuffer str = new StringBuffer();
                    str.append("{");
                    for (int i = 0; i < field.length; i++) {
                        str.append("\"").append(field[i]).append("\"").
                                append(":").append("\"").append(e.get(field[i])).append("\"").append(",");
                    }
                    str.delete(str.length() - 1, str.length());
                    str.append("}");
                    Map<String, String> map = JSON.parseObject(str.toString(), Map.class);
                    return map;
                })
                //分页
                .skip(page * (limit - 1)).limit(limit)
                .collect(Collectors.toList());
        return new ResultEntity(ResultEnum.SUCCESS, result);
    }


    /**
     * 读取excel表格，并存入redis缓存中
     *
     * @return
     */
    public void readExcel(Map<String, Object> objectMap) {

        //表名
        String sheet = objectMap.get("sheet").toString();
        //页码
        Integer page = (Integer) objectMap.get("page");
        //页面数据条数
        Integer limit = (Integer) objectMap.get("limit");

        //excel路径
        String fileName = filepath + sheet;
        //表头数据
        Map<Integer, String> headMap = new HashMap<>();
        //表的数据
        List<Map<Integer, String>> list = null;

        //如果redis里面没有这个表格的缓存数据，读表，存redis
        if (redisUtils.get("full-"+sheet) == null) {
            // 这里 只要，然后读取第一个sheet 同步读取会自动finish
            dataListener = new DataListener();
            //读取excel
            EasyExcel.read(fileName, dataListener).sheet().doRead();
            //获取表头数据
            headMap = dataListener.getHeadMap();
            //获取表的数据
            list = dataListener.getList();

            //整张表的完整数据，存入缓存
            List<Map<String, String>> mapList = this.assembleMap(headMap, list);
            redisUtils.set("full-" + sheet, JSON.toJSONString(mapList));

        }

    }


    /**
     * 删除excel文件,与缓存
     *
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
                return new ResultEntity(ResultEnum.ERROR);
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
        Long del = redisUtils.del(excelname, excelname + "-head");
        if (del > 0) {
            return new ResultEntity(ResultEnum.SUCCESS);
        }

        return new ResultEntity(0, "SYS_EOR", "删除失败");
    }


    /**
     * 组装map
     *
     * @return
     */
    private List<Map<String, String>> assembleMap(Map<Integer, String> head, List<Map<Integer, String>> datalist) {

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

