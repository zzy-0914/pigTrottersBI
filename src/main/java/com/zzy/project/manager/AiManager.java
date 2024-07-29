package com.zzy.project.manager;


import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import com.zzy.project.common.ErrorCode;
import com.zzy.project.exception.BusinessException;
import com.zzy.project.exception.ThrowUtils;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class AiManager {
    @Resource
    private SparkClient sparkClient;
    @Resource
    private YuCongMingClient yuCongMingClient;

    public static final String PRECONDITION = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
            "分析需求：\n" +
            "{数据分析的需求或者目标}\n" +
            "原始数据：\n" +
            "{csv格式的原始数据，用,作为分隔符}\n" +
            "请根据这两部分内容，严格按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）同时不要使用这个符号 '】'\n" +
            "'【【【【【'\n" +
            "{前端 Echarts V5 的 option 配置对象 JSON 代码, 不要生成任何多余的内容，比如注释和代码块标记}\n" +
            "'【【【【【'\n" +
            "{明确的数据分析结论、越详细越好，不要生成多余的注释} \n"
            + "下面是一个具体的例子的模板："+
            "分析需求 网站用户增长速度分析\n" +
            "原始数据如下：num,day\n" +
            "3,12月12日\n" +
            "8,12月13日\n" +
            "5,12月14日\n" +
            "9,9月14日\n" +
            "生成的图表类型是：雷达图"
            + "'【【【【【'\n"
            + "{\"title\":{\"text\":\"网站用户增长速度分析\"},\"tooltip\":{},\"legend\":{\"data\":[\"用户数\"]},\"radar\":{\"indicator\":[{\"name\":\"12月12日\",\"max\":10},{\"name\":\"12月13日\",\"max\":10},{\"name\":\"12月14日\",\"max\":10},{\"name\":\"9月14日\",\"max\":10}]},\"series\":[{\"name\":\"用户数\",\"type\":\"radar\",\"data\":[{\"value\":[3,8,5,9],\"name\":\"用户数\"}]}]}"
            + "'【【【【【'\n" +
            "通过分析，我们可以看出网站的用户增长速度在不同日期有所波动。在12月12日，用户数为3；在12月13日，用户数增长到8，呈现出较高的增长速度；而在12月14日，用户数下降到5，增长速度减缓；最后在9月14日，用户数达到最高点，为9。平均每天的用户数为6.25。总体来看，网站的用户增长速度并不稳定，需要进一步分析原因并采取措施来提高用户增长速度";


    public String doChat(long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        }
        return response.getData().getContent();
    }
    public String sendMesToAIByXH(final String content) {
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(content));
        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传,取值为[1,4096],默认为2048
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                // 指定请求版本，默认使用最新2.0版本
                .apiVersion(SparkApiVersion.V4_0)
                .build();
        // 同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        String responseContent = chatResponse.getContent();
        log.info("星火 AI 返回的结果 {}", responseContent);
        return responseContent;
    }

}
