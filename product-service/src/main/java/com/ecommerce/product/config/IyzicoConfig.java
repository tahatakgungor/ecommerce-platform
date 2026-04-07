package com.ecommerce.product.config;

import com.iyzipay.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IyzicoConfig {

    @Value("${app.iyzico.api-key}")
    private String apiKey;

    @Value("${app.iyzico.secret-key}")
    private String secretKey;

    @Value("${app.iyzico.base-url}")
    private String baseUrl;

    @Bean
    public Options iyzicoOptions() {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);
        return options;
    }
}
