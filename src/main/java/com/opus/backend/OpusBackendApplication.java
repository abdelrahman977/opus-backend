package com.opus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@PropertySource("file:E:/OPUS/application.properties")
//@PropertySource("file:/opt/config/application.properties")
@SpringBootApplication
public class OpusBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpusBackendApplication.class, args);
	}


}
 

