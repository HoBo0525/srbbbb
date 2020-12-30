package com.atguigu.srb.core.controller;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.HttpClientUtils;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.UserInfoVO;
import com.atguigu.srb.core.pojo.vo.WxBindVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.util.CoreProperties;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Hobo
 * @create 2020-12-27 18:55
 */
@Controller
@CrossOrigin
@Api("微信登陆")
@RequestMapping("/api/core/wx")
@Slf4j
public class WxController {
    @Autowired
    UserInfoService userInfoService;
    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping("/login")
    public String getQRCode(HttpSession session){
        String baseUrl = "https://open.weixin.qq.com/connect/qrconnect?" +
                "appid=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=snsapi_login" +
                "&state=%s" +
                "#wechat_redirect ";
        //处理回调url
        String redirectUri = "";
        try {
            redirectUri = URLEncoder.encode(CoreProperties.REDIRECT_URI, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException(ResponseEnum.ERROR, e);
        }
        //处理state：生成随机数，存入session
        String stateUri = UUID.randomUUID().toString();
        log.info("生成了stateUri:" + stateUri);
        session.setAttribute("wx_state", stateUri);

        String qrCodeUrl = String.format(
                baseUrl,
                CoreProperties.APP_ID,
                redirectUri,
                stateUri);

        return "redirect:" + qrCodeUrl;
    }


    //通过回调方法 获取到access_token
    @GetMapping("callback")
    public String callback(String code, String state, HttpSession session){

        //回调被拉起  获得code  state
        log.info("callback被调用");
        log.info("code：" + code);
        log.info("state：" + state);
        log.info("session state:" + session.getAttribute("wx_state"));

        //判断微信回调参数
        Assert.notEmpty(code, ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);
        Assert.notEmpty(state, ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);
        Assert.equals(state, (String)session.getAttribute("wx_state"), ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);

        //获取access_token
        //https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
        String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
        Map<String, String> accessTokenParam = new HashMap<>();
        accessTokenParam.put("appid", CoreProperties.APP_ID);
        accessTokenParam.put("secret", CoreProperties.APP_SECRET);
        accessTokenParam.put("code", code);
        accessTokenParam.put("grant_type", "authorization_code");
        //获取动作
        HttpClientUtils client = new HttpClientUtils(accessTokenUrl, accessTokenParam);

        //发送请求
        String result = "";
        try {
            client.setHttps(true);
            client.get();
            result = client.getContent();
            System.out.println("result:" + result);
        } catch (Exception e) {
            throw  new BusinessException(ResponseEnum.WEIXIN_FETCH_ACCESSTOKEN_ERROR, e);
        }

        //转换获取的result（包括access_token）
//        {
//                "access_token":"ACCESS_TOKEN",
//                "expires_in":7200,
//                "refresh_token":"REFRESH_TOKEN",
//                "openid":"OPENID",
//                "scope":"SCOPE",
//                "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
//        }
        Gson gson = new Gson();
        HashMap<String, Object> resultMap = gson.fromJson(result, HashMap.class);

        //判断获取access_token失败后的响应
        if (resultMap.get("errcode") != null){
            Double errcode = (double)resultMap.get("errcode");
            String errmsg = (String)resultMap.get("errmsg");
            log.error("获取access_token失败 - " + "message: " + errmsg + ", errcode: " + errcode);
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_ACCESSTOKEN_ERROR);
        }

        //获取成功access_token成功
        String accessToken = (String) resultMap.get("access_token");
        String openid = (String) resultMap.get("openid");
        log.info("accessToken:" + accessToken);
        log.info("openid:" + openid);



        //根据openid 查询用户是否注册过
        UserInfo userInfo = userInfoService.getByOpenid(openid);
        //如果用户不存在, 访问微信服务器 获取用户信息， 并且使用微信获取的信息注册用户
        if (userInfo == null){
            log.info("用户不存在,使用微信注册");
            //注册微信新用户
           userInfoService.registerWx(accessToken, openid);
           return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=0";
        }else if (StringUtils.isEmpty(userInfo.getMobile())){
            log.info("微信注册，但是没有绑定手机号码");
            return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=0";
        }else {
            log.info("用户已经用微信注册 并且绑定了手机号码 直接登录");
            return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=1";
        }
    }


    //根据openid  查询用户信息(用于已注册已绑定)
    @ApiOperation("根据openid查询用户信息")
    @ResponseBody
    @GetMapping("/getUserInfoVO/{openid}")
    public R login(
            @ApiParam(value = "微信标识符", required = true)
            @PathVariable String openid){
       UserInfoVO userInfoVO = userInfoService.getUserInfoVOByOpenid(openid);
       return R.ok().data("userInfoVO" , userInfoVO);
    }


    //绑定接口
    @ApiOperation("绑定手机号")
    @ResponseBody
    @PostMapping("/bind")
    public R register(
            @ApiParam(value = "微信绑定对象", required = true)
            @RequestBody WxBindVO wxBindVO,
            HttpServletRequest request){

        //获取前端填写的信息
        String mobile = wxBindVO.getMobile();
        String code = wxBindVO.getCode();

        //校验用户输入得参数
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);
        Assert.notEmpty(code, ResponseEnum.CODE_NULL_ERROR);
        //校验验证码是否正确
        String codeRedis = (String)redisTemplate.opsForValue().get("srb:sms:code:" + mobile);
        Assert.equals(code, codeRedis, ResponseEnum.CODE_ERROR);

        //绑定手机号并登录
        String ip = "";
        if (request.getHeader("x-forwarded-for") == null) {
            ip =request.getRemoteAddr();
        }else {
            ip =request.getHeader("x-forwarded-for");
        }
        UserInfoVO userInfoVO = userInfoService.bind(wxBindVO, ip);

        return R.ok().message("绑定成功").data("userInfoVO", userInfoVO);
    }


    @ApiOperation("微信用户登录")
    @ResponseBody
    @PostMapping("/login")
    public R login(
            @ApiParam(value = "微信绑定对象", required = true)
            @RequestBody WxBindVO wxBindVO,
            HttpServletRequest request) {

        //绑定并登录
        String ip="";
        if (request.getHeader("x-forwarded-for") == null) {
            ip=request.getRemoteAddr();
        }else {
            ip=request.getHeader("x-forwarded-for");
        }
        UserInfoVO userInfoVO = userInfoService.login(wxBindVO, ip);

        return R.ok().message("绑定成功").data("userInfoVO", userInfoVO);
    }



}
