package co.com.bancolombia.datamask.aws.cipher;

import co.com.bancolombia.datamask.cipher.DataCipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import lombok.RequiredArgsConstructor;

import java.util.Base64;

@RequiredArgsConstructor
public class AWSEncryptionSdkCipher implements DataCipher {

    private final AwsCrypto awsCrypto;
    private final JceMasterKey masterKeyProvider;

    @Override
    public String cipher(String inputData) {
        var cipherText = awsCrypto.encryptData(masterKeyProvider, inputData.getBytes()).getResult();
        return new String(Base64.getEncoder().encode(cipherText));
    }

}
