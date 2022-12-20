package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.databind.util.QueryType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdentifyField {
    private String query;
    private QueryType queryType;
}
