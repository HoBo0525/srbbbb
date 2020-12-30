package com.atguigu.srb.core.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description="微信绑定手机号对象")
public class WxBindVO {

        @ApiModelProperty(value = "微信openid")
        private String openid;

        @ApiModelProperty(value = "手机号")
        private String mobile;

        @ApiModelProperty(value = "1：出借人 2：借款人")
        private Integer userType;

        @ApiModelProperty(value = "验证码")
        private String code;
}