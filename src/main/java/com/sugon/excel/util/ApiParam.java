package com.sugon.excel.util;

import lombok.Data;

import java.util.List;

/**
 * API参数类
 * {"page": 1, "limit": 20, "select": "a,b,c", "where": [{"key": "d", "operator": "lt", "value": "2020-07-14"}],
 * "order": [{"key": "d", "by": "desc"}]}
 * @author Wilson 2020/07/14
 */
@Data
public class ApiParam {
    private Integer page = 1;
    private Integer limit = 20;
    private String select;
    private List<ApiWhere> where;
    private List<ApiOrder> order;
}
