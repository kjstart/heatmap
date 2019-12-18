package com.com.demo.heatmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
@ComponentScans({ @ComponentScan("com.com.demo.heatmap.controller") })
@EntityScan(basePackages = { "com.com.demo.heatmap.entity" })
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
