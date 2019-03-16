package com.leo.operatepushlib.http;


/**
 * Create by LEO
 * on 2018/3/16
 * at 17:31
 * in MoeLove Company
 */
public class HttpError extends Exception {
    private int errorCode;

    public HttpError(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
