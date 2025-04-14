package org.example.backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ConfigCloud {
    @Bean
    public Cloudinary configKey(){
        Map<String,String> config = new HashMap<>();
        config.put("cloud_name", "dxg5mvsjd");
        config.put("api_key", "261549774465532");
        config.put("api_secret", "NPArBox6a1OMC-MTevnSs1I77qo");
        return new Cloudinary(config);
    }
}
