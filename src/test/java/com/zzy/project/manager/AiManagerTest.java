package com.zzy.project.manager;


import com.zzy.project.service.ChartService;
import com.zzy.project.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

import java.io.*;
import java.util.List;
import java.util.Map;

@SpringBootTest
class AiManagerTest {
    @Resource
    AiManager aiManager;

    @Resource
    SparkManager sparkManager;
    @Resource
    ChartService chartService;
    @Test
    public void test08() throws IOException{
        String doChat = sparkManager.doChat("你好");
        System.out.println(doChat);
    }
    @Test
    public void test_chat_completions_stream_channel() throws Exception {
        String hi = aiManager.sendMesToAIByXH("你好");
        System.out.println(hi);
    }
    @Test
    public void test() throws IOException {
        File file = new File("工作簿1.xlsx");
        FileInputStream input = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(input);

        // 将 Workbook 内容写入到一个临时文件
        File tempFile = File.createTempFile("temp", ".xlsx");
        FileOutputStream output = new FileOutputStream(tempFile);
        workbook.write(output);

        // 关闭输入输出流
        input.close();
        output.close();

        // 将临时文件转换为 MultipartFile
        InputStream inputStream = new FileInputStream(tempFile);
        byte[] bytes = StreamUtils.copyToByteArray(inputStream);
        MultipartFile multipartFile = new MultipartFileImpl(bytes, file.getName());
        List<Map<Integer, String>> data = ExcelUtils.getData(multipartFile);
        chartService.createTable(2L, multipartFile);
    }
    private static class MultipartFileImpl implements MultipartFile {

        private final byte[] bytes;
        private final String name;

        public MultipartFileImpl(byte[] bytes, String name) {
            this.bytes = bytes;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            // 这里根据实际情况设置 ContentType，例如 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return bytes;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            FileCopyUtils.copy(bytes, dest);
        }
    }
}