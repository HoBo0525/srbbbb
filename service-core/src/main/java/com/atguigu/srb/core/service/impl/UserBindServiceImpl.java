package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.enums.UserBindEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.UserBindMapper;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.UserBindVO;
import com.atguigu.srb.core.service.UserBindService;
import com.atguigu.srb.core.pojo.entity.UserBind;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户绑定表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Service
public class UserBindServiceImpl extends ServiceImpl<UserBindMapper, UserBind> implements UserBindService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Override
    public String commitBindUser(UserBindVO userBindVO, Long userId) {
//        查询srb的user_bind数据库表中身份证号码是否被绑定
//     不是本人user_id的身份证号已存在，则提示已绑定
        QueryWrapper<UserBind> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("id_card", userBindVO.getIdCard())
                .ne("user_id", userId);
        UserBind userBind = baseMapper.selectOne(queryWrapper);
        Assert.isNull(userBind, ResponseEnum.USER_BIND_IDCARD_EXIST_ERROR);


        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        userBind = baseMapper.selectOne(queryWrapper);
//        情况1：数据不存在：创建新的uer_bind记录
        if (userBind == null){
            userBind = new UserBind();
            BeanUtils.copyProperties(userBindVO, userBind);
            userBind.setUserId(userId);
            userBind.setStatus(UserBindEnum.NO_BIND.getStatus());
            baseMapper.insert(userBind);
        }else {
//            情况2：数据存在、但是绑定未完成：修改原有的user_bind记录
            BeanUtils.copyProperties(userBindVO, userBind);
            baseMapper.updateById(userBind);
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentUserId", userId);
        paramMap.put("idCard",userBindVO.getIdCard());
        paramMap.put("personalName", userBindVO.getName());
        paramMap.put("bankType", userBindVO.getBankType());
        paramMap.put("bankNo", userBindVO.getBankNo());
        paramMap.put("mobile", userBindVO.getMobile());
        paramMap.put("returnUrl", HfbConst.USERBIND_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.USERBIND_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        //构建充值自动提交表单
        String formStr = FormHelper.buildFrom(HfbConst.USERBIND_URL, paramMap);
        return formStr;
    }

    @Override
    public void notify(Map<String, Object> paramMap) {
        //托管平台绑定的bindCode
        String bindCode = (String) paramMap.get("bindCode");
        //平台会员id
        String agentUserId = (String) paramMap.get("agentUserId");
        Long userId = Long.parseLong(agentUserId);

        QueryWrapper<UserBind> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserBind userBind = baseMapper.selectOne(queryWrapper);
        userBind.setBindCode(bindCode);
        userBind.setStatus(UserBindEnum.BIND_OK.getStatus());
        baseMapper.updateById(userBind);

        //更新用户表
        UserInfo userInfo = userInfoMapper.selectById(userId);
        userInfo.setBindCode(bindCode);
        userInfo.setName(userBind.getName());
        userInfo.setIdCard(userBind.getIdCard());
        userInfo.setBindStatus(UserBindEnum.BIND_OK.getStatus());
        userInfoMapper.updateById(userInfo);

    }
}
