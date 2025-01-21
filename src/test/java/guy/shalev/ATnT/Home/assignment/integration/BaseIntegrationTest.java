package guy.shalev.ATnT.Home.assignment.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

@SpringBootTest
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Generate unique database name for each test class
        String uniqueDbName = "testdb_" + UUID.randomUUID().toString().replace("-", "");

        // Configure H2 database for testing
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + uniqueDbName + ";DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

        // Configure Hibernate
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");

        // Enable H2 Console
        registry.add("spring.h2.console.enabled", () -> "true");
    }
}