package co.com.bancolombia.datamask.cipher;

public class NoOpCipher implements DataCipher {

    @Override
    public String cipher(String inputData) {
        return inputData;
    }

}
