package co.com.bancolombia.datamask.cipher;

import co.com.bancolombia.datamask.MaskUtils;

public class NoOpCipher implements DataCipher {

    @Override
    public String cipher(String inputData) {
        return MaskUtils.mask(inputData, 0, 4);
    }

}
