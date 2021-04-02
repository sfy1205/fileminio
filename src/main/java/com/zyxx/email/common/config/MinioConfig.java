package com.zyxx.email.common.config;

import com.zyxx.email.common.minio.MinioProp;
import com.zyxx.email.common.minio.MinioUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * minio 核心配置类
 */
@Configuration
@EnableConfigurationProperties(MinioProp.class)
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioUtils creatMinioClient() {
        return new MinioUtils(endpoint, accessKey, secretKey);
    }
}
