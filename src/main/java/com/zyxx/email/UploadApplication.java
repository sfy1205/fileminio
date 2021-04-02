package com.zyxx.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages={"com"})
public class UploadApplication {
    /**
     * 配置文件上传大小
     */
    @Bean
    public MultipartConfigElement getMultiConfig() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.parse("4000MB"));
        factory.setMaxRequestSize(DataSize.parse("4000MB"));
        return factory.createMultipartConfig();
    }

    public static void main(String[] args) {
        SpringApplication.run(UploadApplication.class, args);
    }
}
