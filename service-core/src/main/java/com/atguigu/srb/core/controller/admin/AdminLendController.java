package com.atguigu.srb.core.controller.admin;

import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.service.LendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author Hobo
 * @create 2021-01-10 19:24
 */

@RestController
@Slf4j
@CrossOrigin
@RequestMapping("/admin/core/lend")
@Api(tags = "标的管理")
public class AdminLendController {

    @Autowired
    LendService lendService;

    @ApiOperation("标的列表")
    @GetMapping("/list")
    public R list(){
        List<Lend> lendList = lendService.selectList();
        return  R.ok().data("list", lendList);
    }

    @ApiOperation("获取标的信息")
    @GetMapping("/show/{id}")
    public R show(
            @ApiParam(value = "标的id", required = true)
            @PathVariable Long id){
       Map<String , Object> lendDetail = lendService.getLendDetail(id);
       return R.ok().data("lendDetail", lendDetail);
    }
}
