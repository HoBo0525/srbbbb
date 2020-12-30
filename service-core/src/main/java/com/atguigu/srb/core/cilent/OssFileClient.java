package com.atguigu.srb.core.cilent;

import com.atguigu.common.result.R;
import com.atguigu.srb.core.cilent.fallback.OssFileClientFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Hobo
 * @create 2020-12-30 8:44
 */
@FeignClient(value = "service-oss" , fallback = OssFileClientFallBack.class)
public interface OssFileClient {
    @PostMapping("/api/oss/file/upload-from-url")
    R uploadFromUrl(
            @RequestParam String url,
            @RequestParam String model);
}
