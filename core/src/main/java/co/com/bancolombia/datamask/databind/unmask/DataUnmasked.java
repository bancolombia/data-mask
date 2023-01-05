package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.databind.mask.IdentifyField;
import co.com.bancolombia.datamask.databind.mask.MaskingFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class DataUnmasked {
    private final Object data;
    private Map<IdentifyField, MaskingFormat> fields;
}