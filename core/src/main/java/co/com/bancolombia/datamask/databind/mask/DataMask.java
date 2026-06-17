package co.com.bancolombia.datamask.databind.mask;

import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DataMask<T>(T data, Map<IdentifyField, MaskingFormat> fields) {

    public DataMask(T data, @NonNull List<IdentifyField> defaultFields) {
        this(data, defaultFields.stream()
                .collect(Collectors.toMap(k -> k, v -> new MaskingFormat())));
    }
}