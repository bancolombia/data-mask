package co.com.demo.datamask.aws.config;

import co.com.bancolombia.datamask.aws.AwsConfiguration;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.MaskingObjectMapper;
import co.com.demo.datamask.aws.handler.DemoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Import({ AwsConfiguration.class})
public class LocalExampleConfiguration {

    // --- For Spring Boot 3: ---
    // @Bean
    // @Primary
    // public ObjectMapper objectMapper(DataCipher awsCipher, DataDecipher awsDecipher) {
    //    return new MaskingObjectMapper(awsCipher, awsDecipher);
    // }

    // --- For Spring Boot 4: ---
    // MaskingObjectMapper extends tools.jackson.databind.json.JsonMapper (Jackson 3).
    // Since Spring Boot's CodecsAutoConfiguration also declares a @Primary bean named jacksonJsonMapper
    // of type JsonMapper. We must declare this bean with such a name to avoid a conflict.
    @Bean("jacksonJsonMapper")
    @Primary
    public JsonMapper jacksonJsonMapper(DataCipher awsCipher, DataDecipher awsDecipher) {
        return new MaskingObjectMapper(awsCipher, awsDecipher);
    }

    @Bean
    public RouterFunction<ServerResponse> route(DemoHandler demoHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/creditcard"), demoHandler::queryCreditCard)
                .andRoute(RequestPredicates.POST("/api/creditcard"), demoHandler::receiveCreditCardAgain);
    }


}
