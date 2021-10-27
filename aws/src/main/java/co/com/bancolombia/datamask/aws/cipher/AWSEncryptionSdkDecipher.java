package co.com.bancolombia.datamask.aws.cipher;

import co.com.bancolombia.datamask.cipher.DataDecipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import lombok.RequiredArgsConstructor;

import java.util.Base64;

@RequiredArgsConstructor
public class AWSEncryptionSdkDecipher implements DataDecipher {

    private final AwsCrypto awsCrypto;
    private final JceMasterKey masterKeyProvider;

    @Override
    public String decipher(String inputCipherData) {
        var decodedCipherData = Base64.getDecoder().decode(inputCipherData.getBytes());
        return new String(awsCrypto.decryptData(masterKeyProvider, decodedCipherData).getResult());
    }

}
