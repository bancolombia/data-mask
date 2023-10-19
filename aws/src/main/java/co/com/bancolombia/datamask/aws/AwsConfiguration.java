package co.com.bancolombia.datamask.aws;

import co.com.bancolombia.datamask.aws.cipher.AWSEncryptionSdkCipher;
import co.com.bancolombia.datamask.aws.cipher.AWSEncryptionSdkDecipher;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.secretsmanager.api.GenericManager;
import co.com.bancolombia.secretsmanager.api.exceptions.SecretException;
import co.com.bancolombia.secretsmanager.connector.AWSSecretManagerConnector;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@Configuration
public class AwsConfiguration {

    private static final String DEFAULT_ALGORITHM = "AES";
    private static final String WRAPPING_ALGORITHM = "AES/GCM/NoPadding";

    @Profile({"!local"})
    @Bean(name = "secretManagerSyncConnectorForDataMasking")
    public GenericManager manager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion) {
        return new AWSSecretManagerConnector(secretsRegion);
    }

    @Profile({"local"})
    @Bean(name = "secretManagerSyncConnectorForDataMasking")
    public GenericManager localManager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion,
                                       @Value("${adapters.aws.secrets-manager.endpoint}") String secretsEndpoint) {
        return new AWSSecretManagerConnector(secretsRegion, secretsEndpoint);
    }

    @Bean
    public SecretKey retrieveEncryptionKey(
            @Qualifier("secretManagerSyncConnectorForDataMasking") GenericManager manager,
            @Value("${secrets.dataMaskKey}") String secretWithDataMaskKey) throws SecretException {
        var encryptionKey = manager.getSecret(secretWithDataMaskKey).getBytes();
        return new SecretKeySpec(validateAndDeriveKey(encryptionKey), DEFAULT_ALGORITHM);
    }

    @Bean
    public JceMasterKey masterKeyProvider(SecretKey retrieveEncryptionKey,
                                  @Value("${dataMask.encryptionContext:default_context}") String encryptionContext,
                                  @Value("${dataMask.keyId:}") String keyId) {
        return JceMasterKey.getInstance(retrieveEncryptionKey,
                encryptionContext,
                keyId,
                WRAPPING_ALGORITHM);
    }

    @Bean
    public AwsCrypto awsCrypto() {
        return AwsCrypto.builder()
                .withCommitmentPolicy(CommitmentPolicy.ForbidEncryptAllowDecrypt)
                .withEncryptionAlgorithm(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA256)
                .build();
    }

    @Bean
    @Primary
    public DataCipher dataCipher(AwsCrypto awsCrypto, JceMasterKey masterKeyProvider) {
        return new AWSEncryptionSdkCipher(awsCrypto, masterKeyProvider);
    }

    @Bean
    @Primary
    public DataDecipher dataDecipher(AwsCrypto awsCrypto, JceMasterKey masterKeyProvider) {
        return new AWSEncryptionSdkDecipher(awsCrypto, masterKeyProvider);
    }

    private byte[] validateAndDeriveKey(byte[] key) throws SecretException{
        if (key.length >= 16) {
            byte[] derivedArray;
            switch (key.length) {
                case 16, 24, 32:
                    derivedArray = key;
                    break;
                default:
                    derivedArray = Arrays.copyOfRange(key, 0, 16);
            }
            return derivedArray;
        } else {
            throw new SecretException("Encryption key for data-masking should be at least 16 bytes. Other lengths supported " +
                    "for AES encryption are 24 and 32 bytes. Any given key greater than 16 bytes, and not multiple of 16, " +
                    "will be derived from first 16 bytes.");
        }
    }
}
