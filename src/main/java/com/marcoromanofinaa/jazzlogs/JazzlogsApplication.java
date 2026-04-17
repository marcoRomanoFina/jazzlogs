package com.marcoromanofinaa.jazzlogs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
public class JazzlogsApplication {

	public static void main(String[] args) {
		SpringApplication.run(JazzlogsApplication.class, args);
	}

}
