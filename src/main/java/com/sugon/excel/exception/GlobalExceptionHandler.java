package com.sugon.excel.exception;


import com.sugon.excel.res.ResultEntity;
import com.sugon.excel.res.ResultEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理
 * @author jgz
 * CreateTime 2020/4/23 11:01
 */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 全局异常
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResultEntity<?> globalException(Exception e){
        //业务异常则抛出自定义的异常信息
        if(e instanceof GlobalException){
            return new ResultEntity<>(((GlobalException) e).getStatus(),((GlobalException) e).getCode(),e.getMessage());
        }
        return new ResultEntity<>(ResultEnum.ERROR);
    }

}
