package com.zzy.project.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.zzy.project.service.ChartService;
import lombok.extern.slf4j.Slf4j;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {
    @Resource
    private ChartService chartService;
    public static String excelToCsv(MultipartFile multipartFile){

        List<Map<Integer, String>> list = getData(multipartFile);
        if (CollUtil.isEmpty(list)){
            return "";
        }
        //读取表头,
        StringBuilder stringBuilder = new StringBuilder();
        Map<Integer, String> head = list.get(0);
        List<String> headList = head.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headList, ',')+"\n");
        //将数据加入
        for(int i= 1;i <list.size();i++){
            Map<Integer, String> dataMap = list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ',')+"\n");
        }
        return stringBuilder.toString();
    }


    public static   List<Map<Integer,String>> getData(MultipartFile multipartFile){
        //1.判断是否为空
        if(multipartFile.isEmpty()){
            return null;
        }
        //2.通过easyExcel 获取文件输入流,读取数据
        //2.1 excelType(ExcelTypeEnum.XLSX)指定类型
        //2.2 doReadSync()同步读取数据，并将其转换为 List<Map<Integer, String>> 对象，
        List<Map<Integer,String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("读取失败",e);
        }
        return list ;
    }
}
