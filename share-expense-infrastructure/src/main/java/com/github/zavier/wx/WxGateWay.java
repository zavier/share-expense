package com.github.zavier.wx;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.alibaba.fastjson2.JSON;
import com.github.zavier.dto.wx.WxLoginRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;

@Slf4j
@Component
public class WxGateWay {

    @Resource
    private RestTemplate restTemplate;

    @Value("${wx.appId}")
    private String wxAppId;
    @Value("${wx.appSecret}")
    private String appSecret;

    public String wxLogin(String code) {
        // 简单实现，直接调用微信接口
        final String url = "https://api.weixin.qq.com/sns/jscode2session?appid={1}&secret={2}&js_code={3}&grant_type=authorization_code";
        final ResponseEntity<String> forEntity = restTemplate.getForEntity(url,
                String.class, wxAppId, appSecret, code);
        if (!forEntity.getStatusCode().is2xxSuccessful()) {
            throw new BizException("WX_LOGIN_FAILED", "微信登录失败");
        }
        final String body = forEntity.getBody();
        log.info("wxLoginRespDTO:{}", body);
        Assert.notNull(body, "微信登录失败");
        final WxLoginRespDTO wxLoginRespDTO = JSON.parseObject(body, WxLoginRespDTO.class);
        Assert.isTrue(StringUtils.isNotBlank(wxLoginRespDTO.getOpenid()), "微信登录失败");
        return wxLoginRespDTO.getOpenid();
    }
}
