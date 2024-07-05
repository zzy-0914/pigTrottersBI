package com.zzy.project.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zzy.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author yupi
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 分析目标
     */
    private String goal;
    /**
     * 图表名称
     */
    private String name ;
    /**
     * 图表类型
     */
    private String chartType;



    /**
     * 创建图标用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}