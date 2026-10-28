package com.library.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LibraryApplicationTests {

    @Test
    @DisplayName("Should successfully load complete Spring Boot application context with in-memory test configuration")
    void contextLoads() {
        // Confirms that all Spring Data JPA repositories, services, and web configurations load cleanly
    }
}
