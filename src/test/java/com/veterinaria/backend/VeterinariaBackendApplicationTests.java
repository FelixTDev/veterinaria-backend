package com.veterinaria.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.veterinaria.backend.support.PostgreSqlContainerConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VeterinariaBackendApplicationTests extends PostgreSqlContainerConfiguration {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointRespondsUp() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.message").value("Veterinaria backend operativo"));
	}

}
