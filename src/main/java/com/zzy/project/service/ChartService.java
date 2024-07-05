package com.zzy.project.service;

import com.zzy.project.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author ASUS
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-06-04 15:33:51
*/
public interface ChartService extends IService<Chart> {
   public void createTable(long chart_id, MultipartFile multipartFile);


   boolean updateChartResult(Long chartId, String userSay);

   public void handleChartUpdateError(long chartId, String execMessage);
}
