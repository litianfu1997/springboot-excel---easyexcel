package com.sugon.excel.datalistener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author litianfu
 * @version 1.0
 * @date 2020/7/14 8:21
 * @email 1035869369@qq.com
 */

public class DataListener extends AnalysisEventListener<Map<Integer, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataListener.class);

    private static final int BATCH_COUNT = 5;

    /**
            * 存储表格数据
     */
    private List<Map<Integer, String>> list = new ArrayList<Map<Integer, String>>();

    /**
     * 存储表头数据
     */
    private Map<Integer, String> headMap = new HashMap<>();

    /**
     * 获取每一行表格数据，存储在map里，例如：{0:"李四",1:"22",2:"男",3:"10086",4:"打篮球",5:"南宁"}
     * @param data
     * @param context
     */
    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        list.add(data);
    }


    /**
     * 这里会一行行的返回头
     *
     * @param headMap
     * @param context
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        this.headMap = headMap;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }



    public List<Map<Integer, String>> getList(){
        return list;
    }

    public Map<Integer, String> getHeadMap(){
        return this.headMap;
    }
}
