package com.github.zavier.vo;

import com.alibaba.cola.dto.Response;
import lombok.Data;

@Data
public class ResponseVo {

    private Integer status;

    private String errCode;

    private String msg;

    public static ResponseVo buildFromResponse(Response response) {
        if (response.isSuccess()) {
            return ResponseVo.buildSuccess();
        }
        return ResponseVo.buildFailure(response.getErrCode(), response.getErrMessage());
    }

    public static ResponseVo buildSuccess() {
        ResponseVo response = new ResponseVo();
        response.setStatus(0);
        return response;
    }

    public static ResponseVo buildFailure(String errCode, String errMessage) {
        ResponseVo response = new ResponseVo();
        response.setStatus(-1);
        response.setMsg(errMessage);
        return response;
    }

}
