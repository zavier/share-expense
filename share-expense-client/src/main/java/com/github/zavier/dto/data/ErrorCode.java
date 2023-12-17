package com.github.zavier.dto.data;

public enum ErrorCode{
    PROJECT_NOT_EXIST("PROJECT_NOT_EXIST", "项目不存在");

    private final String errCode;
    private final String errDesc;

    private ErrorCode(String errCode, String errDesc) {
        this.errCode = errCode;
        this.errDesc = errDesc;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrDesc() {
        return errDesc;
    }
}
