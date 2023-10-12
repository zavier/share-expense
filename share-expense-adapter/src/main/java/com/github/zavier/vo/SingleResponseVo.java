package com.github.zavier.vo;

public class SingleResponseVo<T> extends ResponseVo {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static SingleResponseVo buildSuccess() {
        SingleResponseVo response = new SingleResponseVo();
        response.setStatus(0);
        return response;
    }

    public static SingleResponseVo buildFailure(String errCode, String errMessage) {
        SingleResponseVo response = new SingleResponseVo();
        response.setStatus(-1);
        response.setErrCode(errCode);
        response.setMsg(errMessage);
        return response;
    }

    public static <T> SingleResponseVo<T> of(T data) {
        SingleResponseVo<T> response = new SingleResponseVo<>();
        response.setStatus(0);
        response.setData(data);
        return response;
    }

}