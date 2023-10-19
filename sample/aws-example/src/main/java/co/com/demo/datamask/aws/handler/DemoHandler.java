package co.com.demo.datamask.aws.handler;

import co.com.demo.datamask.aws.model.CreditCard;
import co.com.demo.datamask.aws.usecase.CreditCardsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DemoHandler {

    private final CreditCardsUseCase useCase;

    public Mono<ServerResponse> queryCreditCard(ServerRequest serverRequest) {
        return useCase.getCreditCard()
                .flatMap(data -> ServerResponse.ok().bodyValue(data)); // Java object encoded as JSON object. Any
                                                                       // attribute marked for masking and/or encryption
                                                                       // is processed.
    }

    public Mono<ServerResponse> receiveCreditCardAgain(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreditCard.class) // Received JSON payload is decoded again as a Java Object,
                                                          // if any attribute was annotated for encryption, then it should
                                                          // be decrypted back and set to the Java Object as a plain string.
                .flatMap(useCase::receiveCreditCard)
                .flatMap(model -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"msg\": \"Data received and processed, see log to discover plain data\"}"));
    }

}
