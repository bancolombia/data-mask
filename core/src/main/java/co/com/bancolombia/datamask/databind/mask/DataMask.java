package co.com.bancolombia.datamask.databind.mask;

import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class DataMask<T> {
    private final T data;
    private final Map<IdentifyField, MaskingFormat> fields;

    public DataMask(T data, Map<IdentifyField, MaskingFormat> fields){
        this.data = data;
        this.fields = fields;
    }

    public DataMask(T data, @NonNull List<IdentifyField> defaultFields){
        this.data = data;
        this.fields = defaultFields.stream()
                .collect(Collectors.toMap(k -> k, v -> new MaskingFormat()));
    }
}