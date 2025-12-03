package tn.esprit.studentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudentManagementApplication {

    public static void main(String[] args) {
        // The main method is minimal because SpringApplication.run(...) bootstraps
        // the entire Spring Boot application context and starts the embedded server.
        // There is no additional logic here because the application relies on
        // dependency injection and configuration for initialization.

        SpringApplication.run(StudentManagementApplication.class, args);

        /*
         * If you wanted to indicate that this method should not be called directly
         * for testing or other purposes, you could throw an exception, but in
         * a Spring Boot application this is not typical:
         *
         * throw new UnsupportedOperationException("Do not call main() directly");
         *
         * Usually, keep main() minimal and let Spring handle everything.
         */
    }

}
