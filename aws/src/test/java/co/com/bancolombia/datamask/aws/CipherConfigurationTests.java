package co.com.bancolombia.datamask.aws;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CipherConfigurationTests {

    private final AwsConfiguration.CipherConfiguration classUnderTest = new AwsConfiguration.CipherConfiguration();

    @Mock
    AwsCrypto awsCrypto;

    @Mock
    JceMasterKey masterKeyProvider;

    @Test
    void testInstanceOfDataCipher() {
        assertNotNull(classUnderTest.dataCipher(awsCrypto, masterKeyProvider));
    }

    @Test
    void testInstanceOfDataDecipher() {
        assertNotNull(classUnderTest.dataDecipher(awsCrypto, masterKeyProvider));
    }
}
