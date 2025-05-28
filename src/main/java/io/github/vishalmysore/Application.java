package io.github.vishalmysore;



import io.github.vishalmysore.secure.EnableAgentSecurity;
import io.github.vishalmysore.tools4ai.EnableAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication
@EnableAgentSecurity
@EnableAgent
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}