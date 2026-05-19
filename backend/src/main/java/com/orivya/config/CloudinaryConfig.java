package com.orivya.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * CloudinaryConfig — registers Cloudinary SDK as a Spring bean.
 *
 * FILE LOCATION:
 *   backend/src/main/java/com/orivya/config/CloudinaryConfig.java
 *
 * WHY THIS EXISTS:
 *   Render free tier has EPHEMERAL filesystem — any file saved to
 *   uploads/ folder is DELETED on every restart/redeploy.
 *   Cloudinary stores images permanently in the cloud.
 *   Free tier: 25GB storage, 25GB bandwidth/month — enough for Orivya.
 *
 * SETUP STEPS:
 *   1. Go to cloudinary.com → Sign up free
 *   2. Dashboard → copy Cloud Name, API Key, API Secret
 *   3. Add to Render env vars:
 *      CLOUDINARY_CLOUD_NAME = your-cloud-name
 *      CLOUDINARY_API_KEY    = your-api-key
 *      CLOUDINARY_API_SECRET = your-api-secret
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key",    apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure",     "true"); // always use HTTPS URLs
        return new Cloudinary(config);
    }
}