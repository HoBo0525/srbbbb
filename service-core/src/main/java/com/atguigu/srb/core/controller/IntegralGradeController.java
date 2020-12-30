package com.atguigu.srb.core.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 积分等级表 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@RestController
@RequestMapping("/api/core/integralGrade")
public class IntegralGradeController {

    @GetMapping("/list")
    public String listAll(){
        return "list";
    }

}

