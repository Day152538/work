package com.xuyan.fm.common.exception;

import com.xuyan.fm.common.enums.ErrorMsg;

/**
 * 业务异常。Service 层校验失败时抛出，
 * 由 GlobalExceptionHandler 统一转换为 ResultVo 返回前端，
 * 替代原先各处 return ResultVo.fail(...) 与直接把异常信息透传给前端的做法。
 */
public class BusinessException extends RuntimeException {

    private final ErrorMsg errorMsg;

    public BusinessException(ErrorMsg errorMsg) {
        super(errorMsg.getMsg());
        this.errorMsg = errorMsg;
    }

    /** 自定义错误文案（errorMsg 为 null，GlobalExceptionHandler 会用 getMessage()） */
    public BusinessException(String message) {
        super(message);
        this.errorMsg = null;
    }

    public ErrorMsg getErrorMsg() {
        return errorMsg;
    }
}
