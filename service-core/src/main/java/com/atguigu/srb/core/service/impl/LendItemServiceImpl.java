package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.*;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.pojo.entity.TransFlow;
import com.atguigu.srb.core.pojo.entity.UserBind;
import com.atguigu.srb.core.pojo.vo.InvestVO;
import com.atguigu.srb.core.service.LendItemService;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.service.LendService;
import com.atguigu.srb.core.service.TransFlowService;
import com.atguigu.srb.core.service.UserAccountService;
import com.atguigu.srb.core.util.LendNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 标的出借记录表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
@Service
@Slf4j
public class LendItemServiceImpl extends ServiceImpl<LendItemMapper, LendItem> implements LendItemService {

    @Resource
    UserBindMapper userBindMapper;
    @Resource
    LendMapper lendMapper;
    @Resource
    UserAccountService userAccountService;
    @Resource
    LendService lendService;
    @Resource
    TransFlowService transFlowService;
    @Resource
    UserAccountMapper userAccountMapper;

    @Override
    public String commitInvest(InvestVO investVO) {



        //获取标的
        Lend lend = lendMapper.selectById(investVO.getLendId());
        //判断标的状态不是为募资中  则抛出异常
        if (lend.getStatus().intValue() != LendStatusEnum.INVEST_RUN.getStatus().intValue()){
            throw new BusinessException(ResponseEnum.LEND_INVEST_ERROR);
        }

        //判断标的募资金额是否满标
        if (lend.getInvestAmount().doubleValue() >= lend.getAmount().doubleValue()){
            throw new BusinessException(ResponseEnum.LEND_FULL_SCALE_ERROR);
        }

        //判断用户投标金额 与 账户金额
        BigDecimal account = userAccountService.getAccount(investVO.getInvestUserId());
        if (Double.parseDouble(investVO.getInvestAmount()) > account.doubleValue()){
            throw new BusinessException(ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);
        }

        //根据userId获取绑定信息的 bind_code(投资人绑定协议号)
        QueryWrapper<UserBind> userBindQueryWrapper = new QueryWrapper<>();
        userBindQueryWrapper.eq("user_id", investVO.getInvestUserId());
        UserBind userBind = userBindMapper.selectOne(userBindQueryWrapper);
        String voteBindCode = userBind.getBindCode();

        //获取借款人绑定协议号
        userBindQueryWrapper = new QueryWrapper<>();
        userBindQueryWrapper.eq("user_id", lend.getUserId());
        UserBind bind = userBindMapper.selectOne(userBindQueryWrapper);
        String benefitBindCode = bind.getBindCode();

        //添加投标记录
        LendItem lendItem = new LendItem();
        String lendItemNo = LendNoUtils.getLendItemNo();    //生成投资编号
        lendItem.setLendItemNo(lendItemNo);    //投资编号
        lendItem.setLendId(lend.getId());      //标的id
        lendItem.setInvestUserId(investVO.getInvestUserId());   //投资人id
        lendItem.setInvestName(investVO.getInvestName());   //投资人名称
        lendItem.setInvestAmount(new BigDecimal(investVO.getInvestAmount()));   //投资金额
        lendItem.setLendYearRate(lend.getLendYearRate());   //年利率
        lendItem.setInvestTime(LocalDateTime.now());    //投资时间
        lendItem.setLendStartDate(lend.getLendStartDate()); //标的开始日期
        lendItem.setLendEndDate(lend.getLendEndDate()); //标的结束日期

        //预期收益
        //投资金额   年化收益   期数    还款方式
        BigDecimal expectAmount = lendService.getInterestCount(
                lendItem.getInvestAmount(),
                lend.getLendYearRate(),
                lend.getPeriod(),
                lend.getReturnMethod());
        lendItem.setExpectAmount(expectAmount);

        lendItem.setRealAmount(new BigDecimal(0));  //实际收益
        lendItem.setStatus(0);  //状态
        baseMapper.insert(lendItem);


        //封装给 汇付宝 的参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("voteBindCode", voteBindCode);             //投资人绑定协议号
        paramMap.put("benefitBindCode", benefitBindCode);       //借款人协议号
        paramMap.put("agentProjectCode", lend.getLendNo());     //标的编号
        paramMap.put("agentProjectName", lend.getTitle());     //标的名称
        paramMap.put("agentBillNo", lendItemNo);                    //投资订单号
        paramMap.put("voteAmt", investVO.getInvestAmount());    //投资金额
        paramMap.put("votePrizeAmt", new BigDecimal(0));    //投资奖励金
        paramMap.put("voteFeeAmt", new BigDecimal(0));      //商户手续费
        paramMap.put("projectAmt", lend.getAmount());           //标的总需要金额
        paramMap.put("notifyUrl", HfbConst.INVEST_NOTIFY_URL);  //异步通知地址
        paramMap.put("returnUrl", HfbConst.INVEST_RETURN_URL);  //返回平台地址
        paramMap.put("timestamp", RequestHelper.getTimestamp());//时间戳
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);


        //构建提交表单
        String formStr = FormHelper.buildFrom(HfbConst.INVEST_URL, paramMap);
        return formStr;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void notify(Map<String, Object> paramMap) {
        log.info("投资成功");

        //agentBillNo = lendItemNo
        String agentBillNo = (String) paramMap.get("agentBillNo");
        //判断幂等性
        boolean result = transFlowService.isSaveTransFlow(agentBillNo);
        if (result){
            log.info("幂等性反悔");
            return;
        }

        //用户绑定账号
        String bindCode = (String) paramMap.get("voteBindCode");
        //投资金额
        String voteAmt = (String) paramMap.get("voteAmt");
        //根据用户账户修改账户金额：账号金额减少voteAmt、账号冻结金额增加voteAmt
        userAccountMapper.updateAccount(bindCode, new BigDecimal("-" + voteAmt), new BigDecimal(voteAmt));

        //修改投资状态

        QueryWrapper<LendItem> lendItemQueryWrapper = new QueryWrapper<>();
        lendItemQueryWrapper.eq("lend_item_no", agentBillNo);
        LendItem lendItem = baseMapper.selectOne(lendItemQueryWrapper);
        lendItem.setStatus(1);  //已支付
        baseMapper.updateById(lendItem);

        //更新标的信息
        Lend lend = lendMapper.selectById(lendItem.getLendId());
        //标的投资人数
        int investNum = lend.getInvestNum().intValue() + 1;
        lend.setInvestNum(investNum);
        //标的已投金额
        BigDecimal investAmount = lend.getInvestAmount().add(lendItem.getInvestAmount());
        lend.setInvestAmount(investAmount);
        lendMapper.updateById(lend);

        //增加交易流水
        TransFlowBO transFlowBO = new TransFlowBO(
                agentBillNo,
                bindCode,
                new BigDecimal(voteAmt),
                TransTypeEnum.INVEST_LOCK,
                "投资项目编号：" + (String)paramMap.get("agentProjectCode"));
        transFlowService.saveTransFlow(transFlowBO);
    }
}
