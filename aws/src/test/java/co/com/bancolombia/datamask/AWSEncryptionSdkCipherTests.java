package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.aws.cipher.AWSEncryptionSdkCipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSEncryptionSdkCipherTests {

    AWSEncryptionSdkCipher classUnderTest;

    @Mock
    AwsCrypto awsCrypto;

    @Mock
    JceMasterKey masterKeyProvider;

    @Mock
    CryptoResult cryptoResult;

    @BeforeEach
    void prepare() {
        classUnderTest = new AWSEncryptionSdkCipher(awsCrypto, masterKeyProvider);
    }

    @Test
    void testCipherData() {
        when(awsCrypto.encryptData(any(MasterKeyProvider.class), any())).thenReturn(cryptoResult);
        when(cryptoResult.getResult()).thenReturn(new byte[]{});
        assertNotNull(classUnderTest.cipher("hello"));
    }
}
