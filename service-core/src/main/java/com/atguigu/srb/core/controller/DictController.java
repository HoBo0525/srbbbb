package com.atguigu.srb.core.controller;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.atguigu.srb.core.service.DictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Api(tags = "数据字典")
@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/api/core/dict")
public class DictController {

    @Autowired
    DictService dictService;

    @ApiOperation("根据dictCode获取下级节点")
    @GetMapping("/findByDictCode/{dictCode}")
    public R findByDictCode(
            @ApiParam(value = "节点编码", required = true)
            @PathVariable String dictCode){
       List<Dict> list = dictService.findBuDictCode(dictCode);
       return R.ok().data("dictList", list);
    }

}

