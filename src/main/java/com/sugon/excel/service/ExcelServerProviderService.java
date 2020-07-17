package com.sugon.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.sugon.excel.datalistener.DataListener;
import com.sugon.excel.exception.GlobalException;
import com.sugon.excel.util.ApiOrder;
import com.sugon.excel.util.ApiWhere;
import com.sugon.excel.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/16 16:49
 * @email 1035869369@qq.com
 */
@Service
public class ExcelServerProviderService {

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
     * redis工具
     */
    @Autowired
    private RedisUtils redisUtils;

    /**
     *  excel数据源类sql检索
     * json格式：
     * {
     *      "page":0,
     *      "limit": 20,
     *      "select": "*",
     *      "where":[
     *          {"key":"姓名","operator":"lk","value":"李"},
     *          {"key":"日期","operator":"lt","value":"2018年8月1日"}
     *          ],
     *      "order":[
     *          {"key":"日期","by":"desc"}
     *          ],
     *      "sheet":"test1.xlsx"
     * }
     * json字段描述：
     * page：页码
     * limit：该页显示数据条数
     * select：要映射显示的字段
     * where：检索的条件，默认多条件是”与“操作
     * key：条件字段名
     * operator：操作符
     *      eq：等于，lt：小于，gt：大于，lte:大于等于，gte：小于等于
     *      lk：模糊查询，任意匹配 可以使用
     *      ll:模糊查询，左边模糊，如 %徐坤 弃用
     *      lr：模糊查询，右边模糊，如 蔡徐% 建议使用
     *order：排序
     *      key：排序字段
     *      by：排序类型   asc:升序,desc:降序
     * sheet：表格名称
     * @param sheet     表格名称
     * @param select    要映射显示的字段
     * @param page      页码
     * @param limit     该页显示数据条数
     * @param where     检索的条件
     * @param order     排序
     * @return
     */
    public List<Map<String, String>> getExcelData(String sheet, String select, Integer page, Integer limit, List<ApiWhere> where, List<ApiOrder> order) {
        //读取表格数据，并将数据存入redis
        this.readExcel(sheet);

        Map<String, String> fullMap = new HashMap<>();
        String s = redisUtils.get("full-" + sheet);
        List<Map<String, String>> mapList = (List<Map<String, String>>) JSON.parseArray(s, fullMap.getClass());

        return mapList.stream()
                //过滤数据
                .filter(e -> {
                    List<AtomicBoolean> boolList = new ArrayList<>();
                    if (where.size() == 0) {
                        return true;
                    }
                    where.stream().forEach(o -> {
                        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                        String key = (String) o.getKey();
                        String operator = (String) o.getOperator();
                        Object value = o.getValue();
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

                            //左边模糊 %徐坤
                            case "ll":
                                atomicBoolean.set(e.get(key).lastIndexOf(value.toString()) == -1 ? false : true
                                        && e.get(key)
                                        .substring(0,e.get(key).lastIndexOf(value.toString())+value.toString().length())
                                        .endsWith(value.toString()));
                                boolList.add(atomicBoolean);
                                break;

                            //右边模糊 蔡徐%
                            case "lr":
                                atomicBoolean.set(e.get(key).indexOf(value.toString()) == -1 ? false : true
                                        && e.get(key).startsWith(value.toString(), e.get(key).indexOf(value.toString())));
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
                        String key = (String) o.getKey();
                        String by = (String) o.getBy();
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
                    if ("*".equals(select) || select == null) {
                        Set<String> set = e.keySet();
                        field = set.toArray(new String[set.size()]);
                    } else {
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
                .skip((page - 1) * (limit - 1)).limit(limit)
                .collect(Collectors.toList());
    }


    /**
     * 读取excel表格，并存入redis缓存中
     *
     * @return
     */
    private void readExcel(String sheet) {
        //excel路径
        String fileName = filepath + sheet;
        //表头数据
        Map<Integer, String> headMap = new HashMap<>();
        //表的数据
        List<Map<Integer, String>> list = null;

        //如果redis里面没有这个表格的缓存数据，读表，存redis
        if (redisUtils.get("full-" + sheet) == null) {
            // 这里 只要，然后读取第一个sheet 同步读取会自动finish
            InputStream inputStream=null;
            try {
                //访问远程文件
                URL url = new URL(fileName);
                inputStream= url.openStream();
                dataListener = new DataListener();
                //读取excel
                EasyExcel.read(inputStream, dataListener).sheet().doRead();
                //获取表头数据
                headMap = dataListener.getHeadMap();
                //获取表的数据
                list = dataListener.getList();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new GlobalException("文件不存在");
            }

            //整张表的完整数据，存入缓存
            List<Map<String, String>> mapList = this.assembleMap(headMap, list);
            redisUtils.set("full-" + sheet, JSON.toJSONString(mapList));

        }

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
