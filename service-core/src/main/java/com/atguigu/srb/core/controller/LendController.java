package com.atguigu.srb.core.controller;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.service.LendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 标的准备表 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Api(tags = "标的")
@RestController
@RequestMapping("/api/core/lend")
@CrossOrigin
@Slf4j
public class LendController {

    @Autowired
    LendService lendService;

    @ApiOperation("获取标的列表")
    @GetMapping("/list")
    public R list(){
        List<Lend> lendList = lendService.selectList();
        return R.ok().data("lendList", lendList);
    }
}

