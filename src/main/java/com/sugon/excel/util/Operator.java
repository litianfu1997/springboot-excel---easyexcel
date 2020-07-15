package com.sugon.excel.util;

/**
 * 运算符枚举类
 * @author Wilson 2020/07/14
 */
public enum Operator {
    Equals("eq"),
    LikeLeft("ll"),
    LikeRight("lr"),
    GreaterThen("gt"),
    LessThen("lt"),
    GreaterThenEquals("gte"),
    LessThenEquals("lte");

    private String operator;

    Operator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public boolean isValid(String operator) {
        Operator ee = Operator.valueOf(operator);
        return Operator.valueOf(operator) != null;
    }
}