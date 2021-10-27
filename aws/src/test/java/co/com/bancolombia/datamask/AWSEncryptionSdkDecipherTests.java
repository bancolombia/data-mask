package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.aws.cipher.AWSEncryptionSdkCipher;
import co.com.bancolombia.datamask.aws.cipher.AWSEncryptionSdkDecipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSEncryptionSdkDecipherTests {

    AWSEncryptionSdkDecipher classUnderTest;

    @Mock
    AwsCrypto awsCrypto;

    @Mock
    JceMasterKey masterKeyProvider;

    @Mock
    CryptoResult cryptoResult;

    @BeforeEach
    void prepare() {
        classUnderTest = new AWSEncryptionSdkDecipher(awsCrypto, masterKeyProvider);
    }

    @Test
    void testDecipherData() {
        when(awsCrypto.decryptData(any(MasterKeyProvider.class), any(byte[].class))).thenReturn(cryptoResult);
        when(cryptoResult.getResult()).thenReturn(new byte[]{});
        assertNotNull(classUnderTest.decipher("AYABeAQoYI5uMU/f5urhzvSKQdsAAAABAAdFeGFtcGxlAB1SYW5kb21L" +
                "ZXkAAACAAAAADBaxbP4reFbKuBamMAAwPddvvqwKFF+vbwBG4orNGcV1wpj00Sx/CgUphoOYqMyDellBp2kDx1SHRjdvbHJLAgAA" +
                "AAAMAAAQAAAAAAAAAAAAAAAAAHqTEWVGuBygRtCsMwo6g43/////AAAAAQAAAAAAAAAAAAAAAQAAABGbdVOWA9+DJwul4gJLCw5N" +
                "HV+T6dH/co2tZSKx1HtLsa8="));
    }
}
