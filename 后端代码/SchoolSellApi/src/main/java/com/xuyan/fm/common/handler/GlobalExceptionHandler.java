package com.xuyan.fm.common.handler;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.common.exception.ParamException;
import com.xuyan.fm.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 整改点：
 * 1. 原兜底 handler 会把 e.getMessage()（含 SQL 语句、数据库名、连接串等敏感信息）
 *    直接下发给前端，属于信息泄露漏洞；现改为：完整堆栈只写服务端日志，
 *    对外统一返回「系统繁忙」，不泄露任何内部细节。
 * 2. e.printStackTrace() 改为 SLF4J 日志（printStackTrace 不走日志框架，生产环境不可控）。
 * 3. 新增 BusinessException 处理：业务异常的错误码/文案可控下发。
 * 4. 删除 MissingRequestCookieException 处理（Cookie 鉴权已废弃）与文末大段无用注释。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @RequestBody 上的 JSR-303 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVo MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        Map<String, String> collect = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> b));
        return ResultVo.fail(ErrorMsg.PARAM_ERROR, collect);
    }

    /** 请求体缺失或 JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultVo HttpMessageNotReadableExceptionHandler() {
        return ResultVo.fail(ErrorMsg.MISSING_PARAMETER, "requestBody错误!");
    }

    /** URL 中缺少 Query 参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultVo MissingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        return ResultVo.fail(ErrorMsg.MISSING_PARAMETER, "缺少参数" + e.getParameterName());
    }

    /** @RequestParam / @PathVariable 上的约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVo ConstraintViolationExceptionHandler(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> set = e.getConstraintViolations();
        Map<String, String> map = new HashMap<>();
        for (ConstraintViolation<?> cv : set) {
            String[] param = cv.getPropertyPath().toString().split("\\.");
            map.put(param[param.length - 1], cv.getMessage());
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR, map);
    }

    /** 自定义参数异常 */
    @ExceptionHandler(ParamException.class)
    public ResultVo ParamExceptionHandler(ParamException e) {
        return ResultVo.fail(ErrorMsg.PARAM_ERROR, e.getMap());
    }

    /** 业务异常：携带对用户友好的错误文案（如「无权限操作该资源」「订单不存在」） */
    @ExceptionHandler(BusinessException.class)
    public ResultVo BusinessExceptionHandler(BusinessException e) {
        if (e.getErrorMsg() != null) {
            return ResultVo.fail(e.getErrorMsg());
        }
        // 自定义文案的业务异常（errorMsg 为 null），用 getMessage() 作为错误文案
        return ResultVo.fail(ErrorMsg.PARAM_ERROR, e.getMessage());
    }

    /** 请求方法不支持，返回 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResultVo MethodNotSupportedHandler(HttpRequestMethodNotSupportedException e) {
        return ResultVo.fail(ErrorMsg.PARAM_ERROR, "请求方法不支持: " + e.getMethod());
    }

    /** 兜底：任何未预期异常 —— 堆栈只进日志，不下发前端（防信息泄露） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVo> handleException(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultVo.fail(ErrorMsg.SYSTEM_ERROR));
    }
}
