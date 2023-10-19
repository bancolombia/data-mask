package co.com.demo.datamask.aws.usecase;

import co.com.demo.datamask.aws.model.CreditCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditCardsUseCase {

    public Mono<CreditCard> getCreditCard() {
        return Mono.just(new CreditCard("1234 5678 9012 3456",
                "John Doe"));
    }

    public Mono<CreditCard> receiveCreditCard(CreditCard creditCard) {
        return Mono.fromSupplier(() -> {
            System.out.println("Received data (decrypted): " + creditCard);
            return creditCard;
        });
    }
}
