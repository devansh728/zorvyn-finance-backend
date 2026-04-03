package com.financeapp.finance_backend;

import org.springframework.boot.SpringApplication;

public class TestFinanceBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinanceBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
