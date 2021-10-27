package co.com.bancolombia.datamask.cipher;

@FunctionalInterface
public interface DataCipher {
    String cipher(String inputData);
}
