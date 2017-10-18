package com.salesmanager.shop.application;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class ShopApplication extends SpringBootServletInitializer {

    public static AmazonS3 amazonS3Client;
	
    public static void main(String[] args) {
        amazonS3Client = com.amazonaws.services.s3.AmazonS3Client.builder()
                .withRegion(Regions.AP_SOUTHEAST_1)
                .withCredentials(new ProfileCredentialsProvider())
                .build();
        SpringApplication.run(ShopApplication.class, args);
    }
    
    

}
