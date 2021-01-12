package com.atguigu.srb.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ChargeNoUtils {

    public static String getNo() {

        LocalDateTime time=LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String strDate = dtf.format(time);

        String result = "";
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            result += random.nextInt(10);
        }

        return "CHA" + strDate + result;
    }
}