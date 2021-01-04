package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.Dict;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * 数据字典 服务类
 * </p>
 *
 * @author Helen
 * @since 2020-12-12
 */
public interface DictService extends IService<Dict> {

    void importData(InputStream inputStream);

    void exportData(ServletOutputStream outputStream);

    List<Dict> listByParentId(Long parentId);

    List<Dict> findBuDictCode(String dictCode);
}
