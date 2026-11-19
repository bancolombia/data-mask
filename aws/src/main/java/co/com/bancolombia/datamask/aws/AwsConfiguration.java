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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * Registers the AWS Encryption SDK {@link DataCipher} and {@link DataDecipher}. It is loaded by
 * {@link AwsAutoConfiguration} when {@code secrets.dataMaskKey} is set, and can also be imported with
 * {@code @Import(AwsConfiguration.class)}, which requires {@code secrets.dataMaskKey}.
 * <p>
 * Beans of the same type defined by the application take precedence only with auto-configuration:
 * {@code @ConditionalOnMissingBean} sees only the beans processed so far, so with {@code @Import} both beans may be
 * registered, and injecting them fails with {@code NoUniqueBeanDefinitionException}.
 * <p>
 * The Secrets Manager client, the key and the master key are internal: they are not injected by type into the
 * application ({@code defaultCandidate = false}), and they are created only when the cipher beans need them
 * ({@code @Lazy}).
 */
@Configuration
public class AwsConfiguration {

    private static final String DEFAULT_ALGORITHM = "AES";
    private static final String WRAPPING_ALGORITHM = "AES/GCM/NoPadding";

    @Profile({"!local"})
    @Lazy
    @Bean(name = "secretManagerSyncConnectorForDataMasking", defaultCandidate = false)
    public GenericManager manager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion) {
        return new AWSSecretManagerConnector(secretsRegion);
    }

    @Profile({"local"})
    @Lazy
    @Bean(name = "secretManagerSyncConnectorForDataMasking", defaultCandidate = false)
    public GenericManager localManager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion,
                                       @Value("${adapters.aws.secrets-manager.endpoint}") String secretsEndpoint) {
        return new AWSSecretManagerConnector(secretsRegion, secretsEndpoint);
    }

    @Lazy
    @Bean(defaultCandidate = false)
    public SecretKey retrieveEncryptionKey(
            @Qualifier("secretManagerSyncConnectorForDataMasking") GenericManager manager,
            @Value("${secrets.dataMaskKey}") String secretWithDataMaskKey) throws SecretException {
        if (secretWithDataMaskKey.isBlank()) {
            throw new IllegalStateException("secrets.dataMaskKey must not be blank");
        }
        var encryptionKey = manager.getSecret(secretWithDataMaskKey).getBytes();
        return new SecretKeySpec(validateAndDeriveKey(encryptionKey), DEFAULT_ALGORITHM);
    }

    @Lazy
    @Bean(defaultCandidate = false)
    public JceMasterKey masterKeyProvider(@Qualifier("retrieveEncryptionKey") SecretKey retrieveEncryptionKey,
                                  @Value("${dataMask.encryptionContext:default_context}") String encryptionContext,
                                  @Value("${dataMask.keyId:}") String keyId) {
        return JceMasterKey.getInstance(retrieveEncryptionKey,
                encryptionContext,
                keyId,
                WRAPPING_ALGORITHM);
    }

    @Bean
    @ConditionalOnMissingBean
    public AwsCrypto awsCrypto() {
        return AwsCrypto.builder()
                .withCommitmentPolicy(CommitmentPolicy.ForbidEncryptAllowDecrypt)
                .withEncryptionAlgorithm(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA256)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataCipher dataCipher(AwsCrypto awsCrypto,
                                 @Qualifier("masterKeyProvider") JceMasterKey masterKeyProvider) {
        return new AWSEncryptionSdkCipher(awsCrypto, masterKeyProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataDecipher dataDecipher(AwsCrypto awsCrypto,
                                     @Qualifier("masterKeyProvider") JceMasterKey masterKeyProvider) {
        return new AWSEncryptionSdkDecipher(awsCrypto, masterKeyProvider);
    }

    private byte[] validateAndDeriveKey(byte[] key) throws SecretException{
        if (key.length >= 16) {
            return switch (key.length) {
                case 16, 24, 32 -> key;
                default -> Arrays.copyOfRange(key, 0, 16);
            };
        } else {
            throw new SecretException("Encryption key for data-masking should be at least 16 bytes. Other lengths supported " +
                    "for AES encryption are 24 and 32 bytes. Any given key greater than 16 bytes, and not multiple of 16, " +
                    "will be derived from first 16 bytes.");
        }
    }
}
