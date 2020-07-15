package com.sugon.excel.controller;


import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sugon.excel.datalistener.DataListener;
import com.sugon.excel.entity.EntityGenerator;
import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import com.sugon.excel.util.ChineseToSpell;
import com.sugon.excel.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private String excelFile;


    @RequestMapping("/test")
    public ResultEntity test(@RequestBody Map<String, Object> objectMap) {

        String sheet = (String) objectMap.get("sheet");
        String select = (String) objectMap.get("select");
        //页码
        Integer page = (Integer) objectMap.get("page");
        //页面数据条数
        Integer limit = (Integer) objectMap.get("limit");

        List<Map<String, Object>> where = (List<Map<String, Object>>) objectMap.get("where");
        List<Map<String, Object>> order = (List<Map<String, Object>>) objectMap.get("order");

        System.out.println(objectMap);

        Map<String, String> fullMap = new HashMap<>();


        this.excelFile = sheet;

//        String jsonString = redisUtils.get(sheet + "-head");
//        Map<Integer, String> map = JSON.parseObject(jsonString, Map.class);
//        Map<String, String> excelHead = this.getExcelHead(map);
//        System.out.println(excelHead);
//
//        //制作表格对应的实体类,并编译
//        this.createEntityAndCompile(sheet, excelHead);
//        Object excelEntity = this.getExcelEntity();
//        //获取到该实体类所有方法
//        Map<String, Method> excelEntityMethods = this.getExcelEntityMethods(excelEntity);

        String s = redisUtils.get("full-" + this.excelFile);
        List<Map<String, String>> mapList = (List<Map<String, String>>) JSON.parseArray(s, fullMap.getClass());

        List<String> name = mapList.stream().
                filter(e -> {
                    List<AtomicBoolean> boolList = new ArrayList<>();
                    AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                    AtomicBoolean flag = new AtomicBoolean(false);
                    where.stream().forEach(o -> {
                        String key = (String) o.get("key");
                        String operator = (String) o.get("operator");
                        Object value = o.get("value");
                        //等于
                        if (operator.equals("eq")) {
                            atomicBoolean.set(e.get(key).equals(value.toString()));
                            boolList.add(atomicBoolean);
                        }
                        //左边模糊 %福
                        if (operator.equals("ll")) {
                            boolList.add(atomicBoolean);
                        }
                        //右边模糊 李%
                        if (operator.equals("lr")) {
                            boolList.add(atomicBoolean);
                        }
                        //小于
                        if (operator.equals("lt")) {
                            atomicBoolean.set(e.get(key).compareTo(value.toString()) < 0);
                            boolList.add(atomicBoolean);
                        }
                        //大于
                        if (operator.equals("gt")) {
                            atomicBoolean.set(e.get(key).compareTo(value.toString()) > 0);
                            boolList.add(atomicBoolean);
                        }
                        //小于等于
                        if (operator.equals("lte")) {
                            atomicBoolean.set(e.get(key).compareTo(value.toString()) <= 0);
                            boolList.add(atomicBoolean);
                        }
                        //大于等于
                        if (operator.equals("gte")) {
                            atomicBoolean.set(e.get(key).compareTo(value.toString()) >= 0);
                            boolList.add(atomicBoolean);
                        }


                    });
                    for (AtomicBoolean aBoolean : boolList) {
                        if (aBoolean.get() == false) {
                            flag.set(false);
                            break;
                        } else {
                            flag.set(true);
                        }
                    }

                    return flag.get();
                })
                .map(e -> {
                    String[] field = select.split(",");
                    StringBuffer str = new StringBuffer();
                    str.append("{");
                    for (int i = 0; i < field.length; i++) {
                        str.append("\"").append(field[i]).append("\"").
                                append(":").append("\"").append(e.get(field[i])).append("\"").append(",");
                    }
                    str.delete(str.length() - 1, str.length());
                    str.append("}");
                    return str.toString();
                }).collect(Collectors.toList());
        System.out.println(name);


        return new ResultEntity(ResultEnum.SUCCESS, name);
    }


    /**
     * 读取excel表格，并存入redis缓存中
     *
     * @return
     */
    @RequestMapping(value = "/selectRowByAnyKey")
    public ResultEntity selectRowByAnyKey(@RequestBody Map<String, Object> objectMap) {

        //表名
        String sheet = objectMap.get("sheet").toString();
        //页码
        Integer page = (Integer) objectMap.get("page");
        //页面数据条数
        Integer limit = (Integer) objectMap.get("limit");

        if (page == null) {
            page = 0;
        }

        if (limit == null) {
            limit = 20;
        }

        if (sheet == null) {
            return new ResultEntity(0, "SYS_EOR", "该表不存在");
        }
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

            //整张表的完整数据，存入缓存
            List<Map<String, String>> mapList = this.assembleMap(headMap, list);
            redisUtils.set("full-" + sheet, JSON.toJSONString(mapList));

        } else {
            //redis缓存有数据
            //获取表数据
            String dataJsonString = redisUtils.get(sheet);
            //获取表头数据
            String headMapJsonString = redisUtils.get(sheet + "-head");
            //json字符串转换
            list = (List<Map<Integer, String>>) JSON.parseArray(dataJsonString, headMap.getClass());
            headMap = (Map<Integer, String>) JSON.parseObject(headMapJsonString, Map.class);

            //整张表的完整数据,存入缓存
            List<Map<String, String>> mapList = this.assembleMap(headMap, list);
            redisUtils.set("full-" + sheet, JSON.toJSONString(mapList));

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
        })      //分页
                .skip(page * (limit - 1)).limit(limit)
                .collect(Collectors.toList());

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
     * 反射获取ExcelEntity实例
     *
     * @return
     */
    public Object getExcelEntity() {

        Object excelEntity = null;

        try {
            //获取ExcelEntity
            Class<?> clazz = Class.forName("com.sugon.excel.entity.ExcelEntity" +
                    ChineseToSpell.getFullSpell(this.excelFile.split("\\.")[0]));

            excelEntity = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return excelEntity;
    }

    /**
     * 获取ExcelEntity的所有getter和setter方法
     * 调用方式 map.get(方法名)
     *
     * @return
     */
    public Map<String, Method> getExcelEntityMethods(Object excelEntity) {

        String jsonString = redisUtils.get(this.excelFile + "-head");
        Map<Integer, String> map = JSON.parseObject(jsonString, Map.class);

        Map<String, Method> methodsMap = null;
        try {

            //存放字段的map
            Map<String, String> fieldMap = this.getExcelHead(map);

            //存放方法的map
            methodsMap = new HashMap<>();
            Method[] declaredMethods = excelEntity.getClass().getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
            }
            //对fieldMap进行遍历
            Iterator<Map.Entry<String, String>> entries = fieldMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();
                String key = entry.getKey();
                String value = entry.getValue();
                //setter方法
                Method setMethod = null;
                //getter方法
                Method getMethod = null;
                if (ChineseToSpell.isChinese(key)) {
                    setMethod = excelEntity.getClass().
                            getDeclaredMethod("set" + key, String.class);
                    getMethod = excelEntity.getClass().
                            getDeclaredMethod("get" + key, null);
                    methodsMap.put("get" + key, getMethod);
                    methodsMap.put("set" + key, setMethod);
                } else {
                    setMethod = excelEntity.getClass().
                            getDeclaredMethod("set" + key.substring(0, 1).toUpperCase() + key.substring(1), String.class);
                    getMethod = excelEntity.getClass().
                            getDeclaredMethod("get" + key.substring(0, 1).toUpperCase() + key.substring(1), null);
                    methodsMap.put("get" + key.substring(0, 1).toUpperCase() + key.substring(1), getMethod);
                    methodsMap.put("set" + key.substring(0, 1).toUpperCase() + key.substring(1), setMethod);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return methodsMap;
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

    /**
     * 获取每个表头所对应列的名字与值类型
     * 值类型统一为String
     *
     * @return
     */
    public Map<String, String> getExcelHead(Map<Integer, String> excelHead) {
        //表头名类型map
        Map<String, String> typeMap = new HashMap<>();
        //遍历表头map
        Iterator<Map.Entry<Integer, String>> keyEntries = excelHead.entrySet().iterator();
        while (keyEntries.hasNext()) {
            Map.Entry<Integer, String> entry = keyEntries.next();
            Integer key = entry.getKey();
            String value = entry.getValue();
            typeMap.put(value, "String");
        }

        return typeMap;
    }

    /**
     * 制作表格对应的实体类,并编译
     *
     * @param sheet     表名
     * @param excelHead 表头字段名
     */
    private void createEntityAndCompile(String sheet, Map<String, String> excelHead) {
        EntityGenerator entityGenerator = EntityGenerator.getInstance();
        entityGenerator.setMap(excelHead);
        entityGenerator.generator(sheet);
    }


}

