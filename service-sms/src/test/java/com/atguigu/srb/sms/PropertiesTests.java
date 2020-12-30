package com.atguigu.srb.sms;

import com.atguigu.srb.sms.util.SmsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class PropertiesTests {
//
//    @Resource
//    private SmsProperties smsProperties;

    @Test
    public void testPropertiesValue(){

        System.out.println(SmsProperties.KEY_ID);
    }
}
