package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * <p>
 * 用户账户 服务类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
public interface UserAccountService extends IService<UserAccount> {

    String commitCharge(BigDecimal chargeAmt, Long userId);

    void notify(Map<String, Object> paramMap);
}
