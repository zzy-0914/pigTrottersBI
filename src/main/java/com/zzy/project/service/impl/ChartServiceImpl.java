package com.zzy.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.project.common.ErrorCode;
import com.zzy.project.exception.BusinessException;
import com.zzy.project.manager.AiManager;
import com.zzy.project.mapper.ChartMapper;
import com.zzy.project.model.entity.Chart;
import com.zzy.project.service.ChartService;
import com.zzy.project.utils.JsonValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zzy.project.constant.AiId.BI_MODEL_ID;
import static com.zzy.project.constant.StatusConstant.*;
import static com.zzy.project.utils.ExcelUtils.getData;

/**
 * @author yupili
 * @description 针对表【post(帖子)】的数据库操作Service实现
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {

    @Resource
    AiManager aiManager;
    @Resource
    ChartMapper chartMapper;

    /**
     * 将xsl 文件内容转发为字符串存储到数据库中
     * @param chart_id
     * @param multipartFile
     * @return
     */
    @Override
    public   void createTable(long chart_id, MultipartFile multipartFile){
        List<Map<Integer, String>> data = getData(multipartFile);
        //构造sql语句
        StringBuilder sql = new StringBuilder();
        if (CollUtil.isEmpty(data)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"文件中数据为空");
        }

        String chartTableName = "chart_"+chart_id;
        Map<Integer, String> head = data.get(0);
        sql.append("CREATE TABLE ").append(chartTableName).append(" (");
        List<String> heads = head.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        heads.stream().forEach(table_head->sql.append(table_head).append(" varchar(32),"));
        sql.setLength(sql.length() - 1); // 去掉最后一个逗号
        sql.append(");");
        System.out.println(sql);
        boolean table = chartMapper.createTable(sql.toString());
        //插入数据
        //insert into table_name values("","");
        for (int i = 1;i<data.size();i++){
            StringBuilder sql_ = new StringBuilder();
            sql_.append("insert into ").append(chartTableName).append(" values(");
            Map<Integer, String> data_ = data.get(i);
            List<String> dataList = data_.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
            dataList.stream().forEach(table_data->sql_.append("\"").append(table_data).append("\"").append(","));
            sql_.setLength(sql_.length() - 1); // 去掉最后一个逗号
            sql_.append(");");
            System.out.println(sql_);
            chartMapper.createTable(sql_.toString());
        }
    }

    @Override
    public boolean updateChartResult(Long chartId, String userSay) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        //将运行状态改为 running
        updateChart.setStatus(RUNNING);
        boolean b = this.updateById(updateChart);
        if (!b) {
            handleChartUpdateError(chartId, "更新图表执行中状态失败");
            return false;
        }
        // try {
        //     Thread.sleep(10000);
        // } catch (InterruptedException e) {
        //     throw new RuntimeException(e);
        // }
        //进行AI查询
        String result = aiManager.doChat(BI_MODEL_ID, userSay);
        // String result ="1【【【【【{\n" +
        //         "    title: {\n" +
        //         "        text: '网站用户增长趋势分析',\n" +
        //         "        subtext: ''\n" +
        //         "    },\n" +
        //         "    tooltip: {\n" +
        //         "        trigger: 'axis'\n" +
        //         "    },\n" +
        //         "    legend: {\n" +
        //         "        data: ['用户增长数']\n" +
        //         "    },\n" +
        //         "    xAxis: {\n" +
        //         "        type: 'category',\n" +
        //         "        data: ['12月12日', '12月13日', '12月14日', '9月14日']\n" +
        //         "    },\n" +
        //         "    yAxis: {\n" +
        //         "        type: 'value'\n" +
        //         "    },\n" +
        //         "    series: [{\n" +
        //         "        name: '用户增长数',\n" +
        //         "        type: 'line',\n" +
        //         "        data: [3, 8, 5, 9],\n" +
        //         "        markPoint: {\n" +
        //         "            data: [\n" +
        //         "                {type: 'max', name: '最大值'},\n" +
        //         "                {type: 'min', name: '最小值'}\n" +
        //         "            ]\n" +
        //         "        },\n" +
        //         "        markLine: {\n" +
        //         "            data: [\n" +
        //         "                {type: 'average', name: '平均值'}\n" +
        //         "            ]\n" +
        //         "        }\n" +
        //         "    }]\n" +
        //         "}【【【【【3";
        String[] splits = result.split("【【【【【");
        // 拆分之后还要进行校验
        if (splits.length < 3) {
            handleChartUpdateError(chartId, "AI 生成错误");
            return false;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        boolean validJson = JsonValidator.isValidJson(genChart);
        //判断生成的代码是否符合json格式
        if (!validJson){
            handleChartUpdateError(chartId, "更新图表失败");
            return false;
        }
        //生成成功，更新数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(SUCCEED);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult) {
            handleChartUpdateError(chartId, "更新图表失败");
            return false;
        }
        return true;
    }
    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setExecMessage(execMessage);
        updateChartResult.setStatus(FAILED);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult){
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}





