package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.enums.BorrowInfoStatusEnum;
import com.atguigu.srb.core.enums.BorrowerStatusEnum;
import com.atguigu.srb.core.enums.UserBindEnum;
import com.atguigu.srb.core.mapper.*;
import com.atguigu.srb.core.pojo.entity.*;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.service.BorrowInfoService;
import com.atguigu.srb.core.service.BorrowerService;
import com.atguigu.srb.core.service.DictService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 借款信息表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Resource
    UserInfoMapper userInfoMapper;
    @Resource
    IntegralGradeMapper integralGradeMapper;
    @Resource
    DictService dictService;
    @Resource
    BorrowerMapper borrowerMapper;
    @Resource
    private BorrowerService borrowerService;


    @Override
    public BigDecimal getBorrowAmount(Long userId) {
        //获取当前用户积分
        UserInfo userInfo = userInfoMapper.selectById(userId);
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);
        Integer integral = userInfo.getIntegral();

        //根据积分查询额度
        QueryWrapper<IntegralGrade> queryWrapper = new QueryWrapper<>();
        queryWrapper.le("integral_start", integral);
        queryWrapper.ge("integral_end", integral);
        IntegralGrade integralGrade = integralGradeMapper.selectOne(queryWrapper);
        if (integralGrade == null){
            return new BigDecimal("0");
        }

        return integralGrade.getBorrowAmount();

    }

    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, Long userId) {
        //获取用户基本信息
        UserInfo userInfo = userInfoMapper.selectById(userId);

        //判断账户绑定状态
        Assert.isTrue(
                userInfo.getBindStatus().intValue() == UserBindEnum.BIND_OK.getStatus().intValue(),
                ResponseEnum.USER_NO_BIND_ERROR);

        //判断账户审核状态
        Assert.isTrue(
                userInfo.getBorrowAuthStatus().intValue() == BorrowerStatusEnum.AUTH_OK.getStatus().intValue(),
                ResponseEnum.USER_NO_AMOUNT_ERROR);

        //判断账户额度是否足够
        BigDecimal borrowAmount = this.getBorrowAmount(userId);
        Assert.isTrue(
                borrowInfo.getAmount().doubleValue() <= borrowAmount.doubleValue(),
                ResponseEnum.USER_AMOUNT_LESS_ERROR);

        //页面是百分数，数据库表我们是小数，在这里转换
        borrowInfo.setBorrowYearRate(borrowInfo.getBorrowYearRate().divide(new BigDecimal("100")));
        borrowInfo.setUserId(userId);
        baseMapper.insert(borrowInfo);
    }

    @Override
    public Integer getStatusByUserId(Long userId) {
        QueryWrapper<BorrowInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("status").eq("user_id", userId);
        List<Object> objects = baseMapper.selectObjs(queryWrapper);

        if (objects.size() == 0){
            //借款人未提交信息
            return BorrowInfoStatusEnum.CHECK_NO.getStatus();
        }

        Integer status = (Integer)objects.get(0);
        return status;
    }



    private void packageParam(BorrowInfo borrowInfo) {
        String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", borrowInfo.getReturnMethod());
        String moneyUse = dictService.getNameByParentDictCodeAndValue("moneyUse", borrowInfo.getMoneyUse());
        String status = BorrowInfoStatusEnum.getMsgByStatus(borrowInfo.getStatus());
        borrowInfo.getParam().put("returnMethod", returnMethod);
        borrowInfo.getParam().put("moneyUse", moneyUse);
        borrowInfo.getParam().put("status", status);
    }
    @Override
    public List<BorrowInfo> selectList() {
        List<BorrowInfo> borrowInfoList = baseMapper.selectList();
        for (BorrowInfo borrowInfo : borrowInfoList) {
            packageParam(borrowInfo);
        }

        return borrowInfoList;


    }



    @Override
    public Map<String, Object> getBorrowInfoDetail(Long id) {
        //查询借款对象
        BorrowInfo borrowInfo = baseMapper.selectById(id);
        //组装数据
        packageParam(borrowInfo);

        //根据user_id获取借款人对象
        QueryWrapper<Borrower> borrowerQueryWrapper = new QueryWrapper<Borrower>();
        borrowerQueryWrapper.eq("user_id", borrowInfo.getUserId());
        Borrower borrower = borrowerMapper.selectOne(borrowerQueryWrapper);
        //组装借款人对象
        BorrowerDetailVO borrowerDetailVO = borrowerService.getBorrowerDetailVOById(borrower.getId());

        //组装数据
        Map<String, Object> result = new HashMap<>();
        result.put("borrowInfo", borrowInfo);
        result.put("borrower", borrowerDetailVO);
        return result;

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approval(BorrowInfoApprovalVO borrowInfoApprovalVO) {

        //修改借款信息状态
        Long borrowInfoId = borrowInfoApprovalVO.getId();

        BorrowInfo borrowInfo = new BorrowInfo();
        borrowInfo.setId(borrowInfoId);
        borrowInfo.setStatus(borrowInfoApprovalVO.getStatus());
        borrowInfo.setBorrowYearRate(borrowInfoApprovalVO.getLendYearRate().divide(new BigDecimal(100)));
        baseMapper.updateById(borrowInfo);

        //创建标的
        if (borrowInfoApprovalVO.getStatus().intValue() == BorrowInfoStatusEnum.CHECK_OK.getStatus().intValue()){
            //创建标的
            //TODO
        }
    }
}
