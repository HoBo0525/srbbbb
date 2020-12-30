package com.atguigu.srb.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.oss.service.FileService;
import com.atguigu.srb.oss.util.OssProperties;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String upload(InputStream inputStream, String module, String fileName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);


        // /{module}/2020/12/22/uuid.jpg
        String folder = new DateTime().toString("/yyyy/MM/dd/");
        String finalName = UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));
        String path = module + folder + finalName;
        // 上传文件流。
        ossClient.putObject(OssProperties.BUCKET_NAME, path, inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        //https://srb-file-200820.oss-cn-beijing.aliyuncs.com/avatar/05.jpg
        return "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/" + path;
    }

    @Override
    public String upload(String url, String model) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);
        //目录的处理： 分类 + 日期
        String folder = model + new DateTime().toString("/yyyy/MM/dd");

        //文件名的处理
        String fileName = UUID.randomUUID().toString();
        String ext = ".jpg";
        String key = folder + fileName + ext;

        //上传网络流
        InputStream inputStream = null;
        try {
            inputStream = new URL(url).openStream();
        } catch (IOException e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }
        ossClient.putObject(OssProperties.BUCKET_NAME, key, inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        // 返回文件的url
        return "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/" + key;
    }

    @Override
    public void remove(String url) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);

        // objectName ：                                              idcard/2020/12/22/6ab6aaba-5f38-4b7b-a891-f54789e66a6e.jpg
        //url：   https://srb-file-200820.oss-cn-beijing.aliyuncs.com/idcard/2020/12/22/6ab6aaba-5f38-4b7b-a891-f54789e66a6e.jpg
        //host： https://srb-file-200820.oss-cn-beijing.aliyuncs.com/
        String host = "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/";
        String objectName = url.substring(host.length());
        // 删除文件。如需删除文件夹，请将ObjectName设置为对应的文件夹名称。如果文件夹非空，则需要将文件夹下的所有object删除后才能删除该文件夹。
        ossClient.deleteObject(OssProperties.BUCKET_NAME, objectName);

        // 关闭OSSClient。
        ossClient.shutdown();

    }


}
