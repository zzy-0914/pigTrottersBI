package com.zzy.project.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.project.model.entity.Chart;
import org.apache.ibatis.annotations.Update;

/**
* @author ASUS
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-06-04 21:49:43
* @Entity generator.domain.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    @Update("${sql}")
    public boolean createTable(String sql);
}




