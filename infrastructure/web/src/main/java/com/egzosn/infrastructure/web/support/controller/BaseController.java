package com.egzosn.infrastructure.web.support.controller;

//import com.huodull.common.utils.common.Page;
import com.egzosn.infrastructure.web.support.annotation.MsgCode;
import com.egzosn.infrastructure.web.support.exception.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.annotation.Annotation;
import java.util.*;

public class BaseController {

	private static Logger logger = LoggerFactory.getLogger(BaseController.class);
    protected RequesParamFieldSort ppfs = RequesParamFieldSort.newInstance();
    private final static Map<String, MsgCode> errorFields = new WeakHashMap();
    protected static final String CODE_KEY = "code";
    protected static final String MESSAGE_KEY = "message";


    /**
     * @author ZaoSheng
     * Wed Nov 25 10:20:23 CST 2015 ZaoSheng
     */
    protected Map<String, Object> successData() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("success", true);
        data.put(CODE_KEY, 0);
        return data;
    }

    /**
     * @author ZaoSheng
     * Wed Nov 25 10:20:23 CST 2015 ZaoSheng
     */
    protected Map<String, Object> successData(String key, Object result) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(CODE_KEY, 0);
        data.put("success", true);
        data.put(key,result);
        return data;
    }

    /**
     * @author ZaoSheng
     * Wed Nov 25 10:20:23 CST 2015 ZaoSheng
     */
    protected Map<String, Object> newData(Integer code, Object message) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(CODE_KEY, code);
        data.put("success", true);
        data.put(MESSAGE_KEY,message);
        return data;
    }

    /**
     * @author ZaoSheng
     * Wed Nov 25 10:20:23 CST 2015 ZaoSheng
     */
    protected Map<String, Object> throwException(Integer code, Object message) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(CODE_KEY, code);
        data.put("success", false);
        data.put(MESSAGE_KEY,message);
        return data;
    }

    /**
     * @author ZaoSheng
     * Wed Nov 25 10:20:23 CST 2015 ZaoSheng
     */
    protected void throwExceptions(Integer code, String message) {
        throw new CommonException(code, message);
    }



    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleException(Exception ex) throws Exception {

        Map<String, Object> data = new HashMap<>();
        if (ex instanceof BindException) {
            BindException be = (BindException) ex;
            Class<?> targetClass = be.getTarget().getClass();
            List<FieldError> errors = be.getFieldErrors();

            data.put("ZZCode", -1);

            for (FieldError error : errors) {
                String key = error.getField() + error.getCode();
                MsgCode msgCode = null;
                if (null == errorFields.get(key)) {
                    msgCode = targetClass.getDeclaredField(error.getField()).getAnnotation(MsgCode.class);
                    errorFields.put(key, msgCode);
                } else {
                    msgCode = errorFields.get(key);
                }

                if (null != msgCode) {
                    data.put("ZZCode", msgCode.value());
                }
                data.put("message", error.getDefaultMessage());
                return data;
            }

        } else if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException mcve = (ConstraintViolationException) ex;
            Set<ConstraintViolation<?>> constraintViolations = mcve.getConstraintViolations();
            data.put("ZZCode", -1);
            for (ConstraintViolation<?> violation : constraintViolations) {
                data.put("message", violation.getMessage());
              /*  String key = violation.getPropertyPath().toString() + violation.getParameterIndex();
                if (null == errorFields.get(key)){
                    Annotation[] parameterAnnotations = violation.getMethod().getParameterAnnotations()[violation.getParameterIndex()];
                    for (Annotation parameterAnnotation : parameterAnnotations){
                        if (parameterAnnotation instanceof MsgCode){
                            MsgCode code = (MsgCode) parameterAnnotation;
                            errorFields.put(key, code);
                            break;
                        }
                    }
                }*/

//                data.put("ZZCode", errorFields.get(key).value());
                return data;

            }

        } else if (ex instanceof CommonException) {
            CommonException ce = (CommonException) ex;
            data.put("ZZCode", ce.getCode());
            data.put("message", ce.getMessage());
            return data;
        } else {
            throw ex;
        }
        return data;
    }
}


