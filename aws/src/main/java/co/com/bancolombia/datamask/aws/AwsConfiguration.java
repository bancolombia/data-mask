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
import org.springframework.context.annotation.Import;
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
 * It has no stereotype annotation, such as {@code @Configuration}, so component scanning never picks it up:
 * applications that scan {@code co.com.bancolombia} get it through {@link AwsAutoConfiguration}.
 * <p>
 * A {@link DataCipher} or {@link DataDecipher} defined by the application takes precedence only with
 * auto-configuration: {@code @ConditionalOnMissingBean} sees only the beans processed so far, so with {@code @Import}
 * both beans may be registered, and injecting them fails with {@code NoUniqueBeanDefinitionException}.
 * <p>
 * The Secrets Manager client, the key, the master key and the {@link AwsCrypto} are internal: they are not injected by
 * type into the application ({@code defaultCandidate = false}), and they are created only when the cipher beans need
 * them ({@code @Lazy}). Their bean names start with {@code dataMask}, or {@code secretManagerSyncConnectorForDataMasking}
 * for the client, so they don't clash with application beans.
 */
@Lazy
@Import(AwsConfiguration.CipherConfiguration.class)
public class AwsConfiguration {

    private static final String DEFAULT_ALGORITHM = "AES";
    private static final String WRAPPING_ALGORITHM = "AES/GCM/NoPadding";
    private static final String MANAGER = "secretManagerSyncConnectorForDataMasking";
    private static final String ENCRYPTION_KEY = "dataMaskEncryptionKey";
    private static final String MASTER_KEY = "dataMaskMasterKey";
    private static final String AWS_CRYPTO = "dataMaskAwsCrypto";

    @Profile({"!local"})
    @Bean(name = MANAGER, defaultCandidate = false)
    public GenericManager manager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion) {
        return new AWSSecretManagerConnector(secretsRegion);
    }

    @Profile({"local"})
    @Bean(name = MANAGER, defaultCandidate = false)
    public GenericManager localManager(@Value("${adapters.aws.secrets-manager.region}") String secretsRegion,
                                       @Value("${adapters.aws.secrets-manager.endpoint}") String secretsEndpoint) {
        return new AWSSecretManagerConnector(secretsRegion, secretsEndpoint);
    }

    @Bean(name = ENCRYPTION_KEY, defaultCandidate = false)
    public SecretKey retrieveEncryptionKey(
            @Qualifier(MANAGER) GenericManager manager,
            @Value("${secrets.dataMaskKey}") String secretWithDataMaskKey) throws SecretException {
        if (secretWithDataMaskKey.isBlank()) {
            throw new IllegalStateException("secrets.dataMaskKey must not be blank");
        }
        var encryptionKey = manager.getSecret(secretWithDataMaskKey).getBytes();
        return new SecretKeySpec(validateAndDeriveKey(encryptionKey), DEFAULT_ALGORITHM);
    }

    @Bean(name = MASTER_KEY, defaultCandidate = false)
    public JceMasterKey masterKeyProvider(@Qualifier(ENCRYPTION_KEY) SecretKey retrieveEncryptionKey,
                                  @Value("${dataMask.encryptionContext:default_context}") String encryptionContext,
                                  @Value("${dataMask.keyId:}") String keyId) {
        return JceMasterKey.getInstance(retrieveEncryptionKey,
                encryptionContext,
                keyId,
                WRAPPING_ALGORITHM);
    }

    @Bean(name = AWS_CRYPTO, defaultCandidate = false)
    public AwsCrypto awsCrypto() {
        return AwsCrypto.builder()
                .withCommitmentPolicy(CommitmentPolicy.ForbidEncryptAllowDecrypt)
                .withEncryptionAlgorithm(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA256)
                .build();
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

    /**
     * Registers the cipher and the decipher together, or neither: if the application defines one of them, an AWS bean
     * paired with it could not read the values written by the other. Like {@link AwsConfiguration}, it has no stereotype
     * annotation, so component scanning does not pick it up, and the class-level {@code @Lazy} of
     * {@link AwsConfiguration} does not apply to it: the ciphers are created at startup.
     */
    @ConditionalOnMissingBean({DataCipher.class, DataDecipher.class})
    static class CipherConfiguration {

        @Bean
        public DataCipher dataCipher(@Qualifier(AWS_CRYPTO) AwsCrypto awsCrypto,
                                     @Qualifier(MASTER_KEY) JceMasterKey masterKeyProvider) {
            return new AWSEncryptionSdkCipher(awsCrypto, masterKeyProvider);
        }

        @Bean
        public DataDecipher dataDecipher(@Qualifier(AWS_CRYPTO) AwsCrypto awsCrypto,
                                         @Qualifier(MASTER_KEY) JceMasterKey masterKeyProvider) {
            return new AWSEncryptionSdkDecipher(awsCrypto, masterKeyProvider);
        }
    }
}
