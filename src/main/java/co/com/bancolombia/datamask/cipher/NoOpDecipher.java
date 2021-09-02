package co.com.bancolombia.datamask.cipher;

public class NoOpDecipher implements DataDecipher {

    @Override
    public String decipher(String inputData) {
        return inputData;
    }

}
