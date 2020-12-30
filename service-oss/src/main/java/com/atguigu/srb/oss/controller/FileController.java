package com.atguigu.srb.oss.controller;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.oss.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;

@Api(tags = "阿里云文件管理")
@CrossOrigin //跨域
@RestController
@RequestMapping("/api/oss/file")
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @ApiOperation("阿里云文件上传")
    @PostMapping("/upload")
    public R upload(
            @ApiParam(value = "文件所在模块", required = true)
            @RequestParam String module,

            @ApiParam(value = "文件", required = true)
            @RequestParam("file") MultipartFile file){

        try {
            InputStream inputStream = file.getInputStream();
            String name = file.getName();
            String originalFilename = file.getOriginalFilename();
            log.error(name);
            log.error(originalFilename);

            String url = fileService.upload(inputStream, module, originalFilename);

            return R.ok().message("文件上传成功").data("url", url);
        } catch (IOException e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }

    }

    @ApiOperation("阿里云文件删除")
    @DeleteMapping("/remove")
    public R remove(
            @ApiParam(value = "文件", required = true)
            @RequestParam("url") String url){

        fileService.remove(url);
        return R.ok().message("文件删除成功");
    }


    @ApiOperation("微信头像文件上传")
    @PostMapping("/upload-from-url")
    public R uploadFromUrl(
            @RequestParam String url,
            @RequestParam String model){
       String avatar = fileService.upload(url, model);

       return R.ok().message("上传文件头像成功").data("avatar", avatar);
    }
}
