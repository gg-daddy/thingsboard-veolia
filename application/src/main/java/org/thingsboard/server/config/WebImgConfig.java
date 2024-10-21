package org.thingsboard.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebImgConfig implements WebMvcConfigurer  {


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/noauth/img/szy/s1/**").addResourceLocations("file:/root/thingsboard/picture/shizhengyuan/s1/");
        registry.addResourceHandler("/api/noauth/img/szy/navigation/**").addResourceLocations("file:/root/thingsboard/picture/shizhengyuan/navigation/");
        registry.addResourceHandler("/api/noauth/img/szy/s4/**").addResourceLocations("file:/root/thingsboard/picture/shizhengyuan/s4/");
        registry.addResourceHandler("/api/noauth/img/hzx/**").addResourceLocations("file:/root/thingsboard/picture/huizhongxing/");
        registry.addResourceHandler("/api/noauth/img/qt/**").addResourceLocations("file:/root/thingsboard/picture/qiantang/");
    }
}
