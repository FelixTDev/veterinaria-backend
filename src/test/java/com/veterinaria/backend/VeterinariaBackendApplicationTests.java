package com.veterinaria.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@SpringBootTest
class VeterinariaBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
