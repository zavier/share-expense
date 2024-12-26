package com.github.zavier.dto.wx;

import lombok.Data;

// https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
@Data
public class WxLoginRespDTO {
    private String openid;
    private String session_key;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
