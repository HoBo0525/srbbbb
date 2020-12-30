package com.atguigu.srb.core.pojo.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Hobo
 * @create 2020-12-25 20:11
 */
@ApiModel("搜索对象")
@Data
public class UserInfoQuery {
    @ApiModelProperty(value = "关键字")
    private String mobile;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "1：出借人 2：借款人")
    private Integer userType;
}
