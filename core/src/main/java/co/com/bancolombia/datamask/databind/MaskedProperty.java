package co.com.bancolombia.datamask.databind;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaskedProperty {
    private final String masked;
    private final String enc;
}
