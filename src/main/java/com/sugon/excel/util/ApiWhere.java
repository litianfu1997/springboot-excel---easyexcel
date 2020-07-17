package com.sugon.excel.util;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * APIWhere条件类
 * @author Wilson 2020/07/16
 */
@Data
public class ApiWhere {
    private String key;
    private String operator;
    private Object value;

}
