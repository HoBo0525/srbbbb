package com.atguigu.srb.core.cilent.fallback;

import com.atguigu.common.result.R;
import com.atguigu.srb.core.cilent.OssFileClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

/**
 * @author Hobo
 * @create 2020-12-30 12:00
 */
@Service
@Slf4j
public class OssFileClientFallBack implements OssFileClient {
    @Override
    public R uploadFromUrl(String url, String model) {
        log.error("远程调用方法失败， 执行熔断后降级方法");
        return R.ok().data("url", url);
    }
}
