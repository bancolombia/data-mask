package co.com.bancolombia.datamask.aws.cipher;

import co.com.bancolombia.datamask.cipher.DataDecipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.exception.AwsCryptoException;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.Base64;

@RequiredArgsConstructor
@Log
public class AWSEncryptionSdkDecipher implements DataDecipher {

    private final AwsCrypto awsCrypto;
    private final JceMasterKey masterKeyProvider;

    @Override
    public String decipher(String inputCipherData) {
        var decodedCipherData = Base64.getDecoder().decode(inputCipherData.getBytes());
        try {
            var cryptoResult = awsCrypto.decryptData(masterKeyProvider, decodedCipherData);
            return new String(cryptoResult.getResult());
        } catch (AwsCryptoException e) {
            log.severe(e.toString());
            return null;
        }
    }

}
