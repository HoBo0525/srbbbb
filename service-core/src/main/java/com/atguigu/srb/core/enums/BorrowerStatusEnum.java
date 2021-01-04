package com.atguigu.srb.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
//@ToString
public enum BorrowerStatusEnum {

    AUTH_RUN(0, "认证中"),
    AUTH_OK(1, "认证成功"),
    AUTH_FAIL(-1, "认证失败"),
    ;

    private Integer status;
    private String msg;
}