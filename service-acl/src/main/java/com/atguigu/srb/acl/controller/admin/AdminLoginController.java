package com.atguigu.srb.acl.controller.admin;

import com.atguigu.common.result.R;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Api(tags = "用户认证")
@RestController
@RequestMapping("/admin/acl/user")
@Slf4j
@CrossOrigin
public class AdminLoginController {

    /**
     * 登录
     * @return
     */
    @PostMapping("/login")
    public R login() {
        log.info("login");
        return R.ok().data("token","testToken");
    }

    /**
     * 登录后获取用户信息
     * @return
     */
    @GetMapping("/info")
    public R info() {
        log.info("info");
        return R.ok()
                .data("roles","[admin]")
                .data("name","admin")
                .data("avatar","https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
    }


    /**
     * 退出
     * @return
     */
    @PostMapping("/logout")
    public R logout(){
        log.info("logout");
        return R.ok();
    }
}