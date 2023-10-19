package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.aws.AwsConfiguration;
import co.com.bancolombia.secretsmanager.api.GenericManager;
import co.com.bancolombia.secretsmanager.api.exceptions.SecretException;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AwsConfigurationTests {

    private final String TEST_REGION = "us-east-1";
    private final String TEST_ENDPOINT = "http://some-endpoint";
    private final String TEST_SECRET = "secret-name";

    private AwsConfiguration classUnderTest;

    @Mock
    GenericManager genericManager;

    @Mock
    AwsCrypto awsCrypto;

    @Mock
    JceMasterKey masterKeyProvider;

    @BeforeEach
    void prepare() {
        classUnderTest = new AwsConfiguration();
        System.setProperty("aws.region", "us-east-1");
    }

    @Test
    void testGetSecretsGenericManager() {
        assertNotNull(classUnderTest.manager(TEST_REGION));
    }

    @Test
    void testGetLocalSecretsGenericManager() {
        assertNotNull(classUnderTest.localManager(TEST_REGION, TEST_ENDPOINT));
    }

    @Test
    void testInstanceOfAwsCrypto() {
        assertNotNull(classUnderTest.awsCrypto());
    }

    @Test
    void testInstanceOfDataCipher() {
        assertNotNull(classUnderTest.dataCipher(awsCrypto, masterKeyProvider));
    }

    @Test
    void testInstanceOfDataDecipher() {
        assertNotNull(classUnderTest.dataDecipher(awsCrypto, masterKeyProvider));
    }

    @ParameterizedTest
    @CsvSource({
            "1234567890123456",
            "123456789012345678901234",
            "12345678901234567890123456789012",
            "12345678901234567890",
    })
    void testGenerateSecretKey(String key) throws SecretException {
        when(genericManager.getSecret(anyString())).thenReturn(key);
        assertNotNull(classUnderTest.retrieveEncryptionKey(genericManager, TEST_SECRET));
    }

    @Test
    void testFailGenerateSecretKey() throws SecretException {
        when(genericManager.getSecret(anyString())).thenReturn("1234");
        assertThrows(SecretException.class, () -> {
            classUnderTest.retrieveEncryptionKey(genericManager, TEST_SECRET);
        });
    }


}
