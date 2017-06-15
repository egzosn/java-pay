package com.egzosn.infrastructure.web.support.service;


import com.egzosn.infrastructure.web.support.exception.CommonException;

/**
 * Created by egan on 2014/11/6.
 */
public abstract class SupportService {


    protected void throwError(Integer code, String message) {
        throw new CommonException(code, message);
    }

    protected void throwError(Integer code) {
        throwError(code, "");
    }



}
