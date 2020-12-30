package com.atguigu.srb.core.util;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="wx.open")
public class CoreProperties implements InitializingBean {
    private String appId;
    private String appSecret;
    private String redirectUri;

    public static String APP_ID;
    public static String APP_SECRET;
    public static String REDIRECT_URI;

    //当私有成员被赋值后，此方法自动被调用，从而初始化常量
    @Override
    public void afterPropertiesSet() throws Exception {
        APP_ID = appId;
        APP_SECRET = appSecret;
        REDIRECT_URI = redirectUri;
    }
}