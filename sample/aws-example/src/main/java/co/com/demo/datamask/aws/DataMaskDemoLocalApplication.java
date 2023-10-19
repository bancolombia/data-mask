package co.com.demo.datamask.aws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataMaskDemoLocalApplication {
    public static void main(String[] args) {
        System.setProperty("aws.region", "us-east-1");
        SpringApplication.run(DataMaskDemoLocalApplication.class, args);
    }
}
