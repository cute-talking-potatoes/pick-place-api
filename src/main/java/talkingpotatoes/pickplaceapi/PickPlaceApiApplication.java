package talkingpotatoes.pickplaceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class PickPlaceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickPlaceApiApplication.class, args);
    }

}
