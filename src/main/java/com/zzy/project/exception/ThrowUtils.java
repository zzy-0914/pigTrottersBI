package com.zzy.project.exception;

import com.zzy.project.common.ErrorCode;

/**
 * 负责对异常的包装
 */
public class ThrowUtils {
    public static void throwIf(boolean condition , RuntimeException runtimeException){
        if (condition){
            throw runtimeException;
        }
    }
    public static void  throwIf(boolean condition,ErrorCode errorCode){
        throwIf(condition,new BusinessException(errorCode));
    }

    /**
     * 带描述错误信息信息
     * @param condition
     * @param errorCode
     * @param mess
     */
    public static void  throwIf(boolean condition,ErrorCode errorCode,String mess){
        throwIf(condition,new BusinessException(errorCode,mess));
    }
}
