package co.com.demo.datamask.aws.config;

import co.com.bancolombia.datamask.aws.AwsConfiguration;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.MaskingObjectMapper;
import co.com.demo.datamask.aws.handler.DemoHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@Import({ AwsConfiguration.class})
public class LocalExampleConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper(DataCipher awsCipher, DataDecipher awsDecipher) {
        return new MaskingObjectMapper(awsCipher, awsDecipher);
    }

    @Bean
    public RouterFunction<ServerResponse> route(DemoHandler demoHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/creditcard"), demoHandler::queryCreditCard)
                .andRoute(RequestPredicates.POST("/api/creditcard"), demoHandler::receiveCreditCardAgain);
    }


}
