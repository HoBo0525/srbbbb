package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.HttpClientUtils;
import com.atguigu.common.util.MD5;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.cilent.OssFileClient;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.mapper.UserLoginRecordMapper;
import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.atguigu.srb.core.pojo.entity.UserLoginRecord;
import com.atguigu.srb.core.pojo.query.UserInfoQuery;
import com.atguigu.srb.core.pojo.vo.LoginVO;
import com.atguigu.srb.core.pojo.vo.RegisterVO;
import com.atguigu.srb.core.pojo.vo.UserInfoVO;
import com.atguigu.srb.core.pojo.vo.WxBindVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户基本信息 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private UserAccountMapper userAccountMapper;
    @Resource
    private UserLoginRecordMapper userLoginRecordMapper;
    @Resource
    OssFileClient ossFileClient;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void register(RegisterVO registerVO) {

        //判断用户是否被注册
        //创建条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", registerVO.getMobile());
        //根据条件查询
        Integer count = baseMapper.selectCount(queryWrapper);
        //MOBILE_EXIST_ERROR(-207, "手机号已被注册"),
        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        //插入用户基本信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserType(registerVO.getUserType());
        userInfo.setNickName(registerVO.getMobile());
        userInfo.setName(registerVO.getMobile());
        userInfo.setMobile(registerVO.getMobile());
        userInfo.setPassword(MD5.encrypt(registerVO.getPassword()));
        userInfo.setStatus(UserInfo.STATUS_NORMAL); //正常
        //设置用户默认头像
        userInfo.setHeadImg("https://srb-file-200820.oss-cn-beijing.aliyuncs.com/avatar/05.jpg");
        baseMapper.insert(userInfo);

        //创建会员账户
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountMapper.insert(userAccount);
    }

    @Override
    public UserInfoVO login(LoginVO loginVO, String ip) {
        String mobile = loginVO.getMobile();
        String password = loginVO.getPassword();

        //获取会员
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", mobile);
        queryWrapper.eq("user_type", loginVO.getUserType());
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);

        //用户不存在
        //LOGIN_MOBILE_ERROR(-208, "用户不存在"),
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);

        //校验密码
        //LOGIN_PASSWORD_ERROR(-209, "密码不正确"),
        Assert.equals(MD5.encrypt(password), userInfo.getPassword(), ResponseEnum.LOGIN_PASSWORD_ERROR);

        //用户是否被禁用
        //LOGIN_DISABLED_ERROR(-210, "用户已被禁用"),
        Assert.equals(userInfo.getStatus(), UserInfo.STATUS_NORMAL, ResponseEnum.LOGIN_DISABLED_ERROR);

        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecord.setIp(ip);
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO(
                userInfo.getName(),
                userInfo.getNickName(),
                userInfo.getUserType(),
                token);


        return userInfoVO;

    }

    @Override
    public IPage<UserInfo> listPage(Page<UserInfo> pageParam, UserInfoQuery userInfoQuery) {

        String mobile = userInfoQuery.getMobile();
        Integer status = userInfoQuery.getStatus();
        Integer userType = userInfoQuery.getUserType();

        QueryWrapper<UserInfo> userInfoqueryWrapper = new QueryWrapper<>();

        if(userInfoQuery == null){
            return baseMapper.selectPage(pageParam, null);
        }

        userInfoqueryWrapper
                .like(StringUtils.isNotBlank(mobile), "mobile", mobile)
                .eq(status != null, "status", userInfoQuery.getStatus())
                .eq(userType != null, "user_type", userType);
        return baseMapper.selectPage(pageParam, userInfoqueryWrapper);
    }

    @Override
    public void lock(Long id, Integer status) {
        UserInfo userInfo = baseMapper.selectById(id);
        userInfo.setStatus(status);
        baseMapper.updateById(userInfo);
    }

    //使用微信标识符openid 查询用户是否存在
    @Override
    public UserInfo getByOpenid(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        return userInfo;
    }

    //获取用户的微信信息
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfo registerWx(String accessToken, String openid) {

        //向微信的资源服务器发起请求， 根据access_token获取当前的用户微信信息
        //http请求方式: GET
        //https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
        String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo";
        Map<String, String> baseUserInfoParam = new HashMap<>();
        baseUserInfoParam.put("access_token", accessToken);
        baseUserInfoParam.put("openid", openid);
        HttpClientUtils client = new HttpClientUtils(baseUserInfoUrl, baseUserInfoParam);

        //取到用户微信信息
        String resultUserInfo = "";

        try {
            client.setHttps(true);
            client.get();
            resultUserInfo = client.getContent();
        } catch (Exception e) {
            //微信用户信息获取失败
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_USERINFO_ERROR,e);
        }

        Gson gson = new Gson();
        HashMap<String, Object> resultUserInfoMap = gson.fromJson(resultUserInfo, HashMap.class);
        //获取信息响应失败后
        if (resultUserInfoMap.get("errmsg") != null){
            double errcode = (double) resultUserInfoMap.get("errcode");
            String errmsg = (String) resultUserInfoMap.get("errmsg");
            log.error("获取微信用户信息失败 - " + "message: " + errmsg + ", errcode: " + errcode);
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_USERINFO_ERROR);
        }
        //获取信息成功 提出用户信息
        String nickname = (String) resultUserInfoMap.get("nickname");
        String headimgurl = (String) resultUserInfoMap.get("headimgurl");


        R r = ossFileClient.uploadFromUrl(headimgurl, "srb");
        String url = (String) r.getData().get("url");
        System.err.println(url);
        //注册新用户
        UserInfo userInfo = new UserInfo();
        userInfo.setOpenid(openid);
        userInfo.setHeadImg(url);
        userInfo.setNickName(nickname);
        userInfo.setName(nickname);
        userInfo.setUserType(UserInfo.STATUS_NORMAL);
        baseMapper.insert(userInfo);

        //把新用户的信息添加到User_Account
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountMapper.insert(userAccount);

        return userInfo;
    }

    @Override
    public UserInfoVO getUserInfoVOByOpenid(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);

        UserInfoVO userInfoVO = new UserInfoVO();
//        userInfoVO.setName(userInfo.getName());
//        userInfoVO.setNickName(userInfo.getNickName());
//        userInfoVO.setUserType(userInfo.getUserType());
//        userInfoVO.setMobile(userInfo.getMobile());
//        userInfoVO.setHeadImg(userInfo.getHeadImg());
        BeanUtils.copyProperties(userInfo,userInfoVO);

        return userInfoVO;
    }

    @Override
    public UserInfoVO bind(WxBindVO wxBindVO, String remoteAddr) {

        //判断用户绑定的手机号 是否被注册
        String mobile = wxBindVO.getMobile();
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", mobile);
        Integer count = baseMapper.selectCount(queryWrapper);
        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        //根据openid 获取用户信息
        String openid = wxBindVO.getOpenid();
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);

        //绑定手机号
        userInfo.setMobile(mobile);
        userInfo.setUserType(wxBindVO.getUserType());
        baseMapper.updateById(userInfo);


        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setIp(remoteAddr);
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(userInfo.getMobile());
        userInfoVO.setUserType(wxBindVO.getUserType());

        return userInfoVO;
    }

    @Transactional( rollbackFor = {Exception.class})
    @Override
    public UserInfoVO login(WxBindVO wxBindVO, String remoteAddr) {

        String mobile = wxBindVO.getMobile();
        Integer userType = wxBindVO.getUserType();
        String openid = wxBindVO.getOpenid();

        System.err.println("mobile = " + mobile);
        //查看用户是否存在
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper
                .eq("mobile", mobile)
                .eq("user_type", userType)
                .eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(userInfoQueryWrapper);

        //用户不存在
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);

        //用户是否被禁用
        Assert.equals(userInfo.getStatus(), UserInfo.STATUS_NORMAL, ResponseEnum.LOGIN_DISABLED_ERROR);

        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setIp(remoteAddr);
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(userInfo.getMobile());
        userInfoVO.setUserType(userType);

        return userInfoVO;
    }
}