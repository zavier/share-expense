package com.github.zavier.vo;

import com.alibaba.cola.dto.PageResponse;

import java.util.List;

public class PageResponseVo<T> extends ResponseVo {

    private PageData<T> data;

    public PageData<T> getData() {
        return data;
    }

    public void setData(PageData<T> data) {
        this.data = data;
    }

    public static <T> PageResponseVo<T> buildFromPageResponse(PageResponse<T> pageResponse) {
        if (pageResponse.isSuccess()) {
            return of(pageResponse.getData(), pageResponse.getTotalCount());
        }
        return buildFailure(pageResponse.getErrCode(), pageResponse.getErrMessage());
    }


    public static PageResponseVo buildSuccess() {
        PageResponseVo response = new PageResponseVo();
        response.setStatus(0);
        response.setData(PageData.empty());
        return response;
    }

    public static PageResponseVo buildFailure(String errCode, String errMessage) {
        PageResponseVo response = new PageResponseVo();
        response.setStatus(-1);
        response.setErrCode(errCode);
        response.setMsg(errMessage);
        return response;
    }

    public static <T> PageResponseVo<T> of(List<T> data, int totalCount) {
        PageResponseVo<T> response = new PageResponseVo<>();
        response.setStatus(0);
        response.setData(new PageData<>(data, totalCount));
        return response;
    }

}
