package co.com.bancolombia.datamask.databind.unmask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class DataUnmasked {
    private final Object data;
    private List<String> fields;
}