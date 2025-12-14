package com.dbSentinel.DB_Sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class DbSentinelApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbSentinelApplication.class, args);
	}

}
