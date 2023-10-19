package co.com.demo.datamask.aws.model;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreditCard {
    @Mask(rightVisible=4, queryOnly = TransformationType.ALL, format = DataMaskingConstants.ENCRYPTION_AS_OBJECT)
    private String number;
    private String clientName;
}
