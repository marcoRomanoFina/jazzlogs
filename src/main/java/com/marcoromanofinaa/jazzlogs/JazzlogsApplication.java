package com.marcoromanofinaa.jazzlogs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class JazzlogsApplication {

	public static void main(String[] args) {
		SpringApplication.run(JazzlogsApplication.class, args);
	}

}
