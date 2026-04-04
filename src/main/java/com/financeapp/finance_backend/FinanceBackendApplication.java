package com.financeapp.finance_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinanceBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceBackendApplication.class, args);
	}

}
