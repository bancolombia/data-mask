package co.com.bancolombia.datamask.databind.mask;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdentifyField {
    private String query;
    private QueryType queryType;
}
