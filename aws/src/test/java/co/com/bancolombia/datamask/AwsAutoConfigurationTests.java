package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.aws.AwsAutoConfiguration;
import co.com.bancolombia.datamask.aws.AwsConfiguration;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import co.com.bancolombia.secretsmanager.api.GenericManager;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unless a test says otherwise, beans are initialized lazily, so the context starts without calling AWS Secrets
 * Manager.
 */
class AwsAutoConfigurationTests {

    private static final String REGION = "adapters.aws.secrets-manager.region=us-east-1";
    private static final String DATA_MASK_KEY = "secrets.dataMaskKey=data-mask-key";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class, AwsAutoConfiguration.class))
            .withInitializer(context -> context.addBeanFactoryPostProcessor(
                    new LazyInitializationBeanFactoryPostProcessor()))
            .withPropertyValues(REGION);

    @Test
    void shouldListOnlyAwsAutoConfigurationAsAutoConfiguration() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(AwsAutoConfiguration.class.getName())
                .doesNotContain(AwsConfiguration.class.getName());
    }

    @Test
    void shouldRegisterCipherBeansWhenDataMaskKeyIsSet() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("dataCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("dataDecipher");
                });
    }

    @Test
    void shouldNotRegisterBeansWithoutDataMaskKey() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(AwsConfiguration.class)
                .doesNotHaveBean(DataCipher.class)
                .doesNotHaveBean(DataDecipher.class));
    }

    @Test
    void shouldKeepCipherBeansDefinedByTheApplicationWithoutCallingSecretsManager() {
        // Not lazy: the context would fail on AWS credentials if the key were fetched.
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        PropertyPlaceholderAutoConfiguration.class, AwsAutoConfiguration.class))
                .withPropertyValues(REGION, DATA_MASK_KEY)
                .withUserConfiguration(CustomCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("customDecipher");
                    assertThat(context.getBeanFactory().containsSingleton("retrieveEncryptionKey")).isFalse();
                    assertThat(context.getBeanFactory()
                            .containsSingleton("secretManagerSyncConnectorForDataMasking")).isFalse();
                });
    }

    @Test
    void shouldRegisterBeansOnceWhenAlsoImported() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ImportingConfiguration.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(DataCipher.class)
                        .hasSingleBean(DataDecipher.class));
    }

    @Test
    void shouldFailAtStartupWhenImportedWithoutDataMaskKey() {
        // Not lazy, like a real application: the import is not skipped when the property is missing.
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues(REGION)
                .withUserConfiguration(ImportingConfiguration.class)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().hasMessageContaining("secrets.dataMaskKey"));
    }

    @Test
    void shouldFailAtStartupWhenDataMaskKeyIsBlank() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        PropertyPlaceholderAutoConfiguration.class, AwsAutoConfiguration.class))
                .withPropertyValues(REGION, "secrets.dataMaskKey= ")
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("secrets.dataMaskKey must not be blank"));
    }

    @Test
    void shouldNotPreferTheLibraryCipherWhenImportedBeforeTheApplicationCipher() {
        // With @Import, @ConditionalOnMissingBean does not see beans of classes processed later, so both beans are
        // registered. Injecting them by type must fail instead of silently using the AWS cipher.
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ImportingConfiguration.class, CustomCipherConfiguration.class)
                .run(context -> {
                    assertThat(context.getBeanNamesForType(DataCipher.class))
                            .containsExactlyInAnyOrder("dataCipher", "customCipher");
                    assertThatThrownBy(() -> context.getBean(DataCipher.class))
                            .isInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldNotInjectInternalBeansByTypeIntoTheApplication() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ApplicationBeansConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ApplicationBeansConsumer consumer = context.getBean(ApplicationBeansConsumer.class);
                    assertThat(consumer.manager()).isSameAs(context.getBean("applicationManager"));
                    assertThat(consumer.secretKey()).isSameAs(context.getBean("applicationSecretKey"));
                });
    }

    @Test
    void shouldCipherWithTheKeyOfTheSecretsManagerClient() {
        new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues(REGION, DATA_MASK_KEY)
                .withUserConfiguration(ImportingWithFakeManagerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    String encrypted = context.getBean(DataCipher.class).cipher("hello");
                    assertThat(context.getBean(DataDecipher.class).decipher(encrypted)).isEqualTo("hello");
                });
    }

    @Test
    void shouldKeepAwsConfigurationOutOfComponentScanning() {
        // Applications whose base package is co.com.bancolombia scan AwsConfiguration's package.
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ScanningConfiguration.class, CustomCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("customDecipher");
                });
        contextRunner.withUserConfiguration(ScanningConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(DataCipher.class));
    }

    @Test
    void shouldNotRegisterLibraryCiphersWhenTheApplicationDefinesOnlyOne() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(OnlyCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
                    assertThat(context).doesNotHaveBean(DataDecipher.class);
                });
    }

    @Test
    void shouldKeepTheLibraryAwsCryptoNextToTheApplicationOne() {
        new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues(REGION, DATA_MASK_KEY)
                .withUserConfiguration(ImportingWithFakeManagerConfiguration.class,
                        ApplicationAwsCryptoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AwsCrypto libraryCrypto = context.getBean("awsCrypto", AwsCrypto.class);
                    assertThat(libraryCrypto.getEncryptionAlgorithm())
                            .isEqualTo(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA256);
                    assertThat(context.getBean(AwsCrypto.class)).isSameAs(context.getBean("applicationCrypto"));
                    assertThat(context.getBean(DataCipher.class)).extracting("awsCrypto").isSameAs(libraryCrypto);
                    assertThat(context.getBean(DataDecipher.class)).extracting("awsCrypto").isSameAs(libraryCrypto);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCipherConfiguration {
        @Bean
        DataCipher customCipher() {
            return new NoOpCipher();
        }

        @Bean
        DataDecipher customDecipher() {
            return new NoOpDecipher();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "co.com.bancolombia.datamask.aws", excludeFilters =
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class))
    static class ScanningConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    static class OnlyCipherConfiguration {
        @Bean
        DataCipher customCipher() {
            return new NoOpCipher();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationAwsCryptoConfiguration {
        @Bean
        AwsCrypto applicationCrypto() {
            return AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AwsConfiguration.class)
    static class ImportingConfiguration {
    }

    record ApplicationBeansConsumer(GenericManager manager, SecretKey secretKey) {
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationBeansConfiguration {
        @Bean
        GenericManager applicationManager() {
            return new FakeManager();
        }

        @Bean
        SecretKey applicationSecretKey() {
            return new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
        }

        @Bean
        ApplicationBeansConsumer applicationBeansConsumer(GenericManager manager, SecretKey secretKey) {
            return new ApplicationBeansConsumer(manager, secretKey);
        }
    }

    /**
     * Replaces the Secrets Manager client of {@link AwsConfiguration}, registered before it, by name.
     */
    @Configuration(proxyBeanMethods = false)
    @Import({AwsConfiguration.class, FakeManagerConfiguration.class})
    static class ImportingWithFakeManagerConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeManagerConfiguration {
        @Bean(name = "secretManagerSyncConnectorForDataMasking", defaultCandidate = false)
        GenericManager fakeManager() {
            return new FakeManager();
        }
    }

    static class FakeManager implements GenericManager {
        @Override
        public String getSecret(String secretName) {
            return "0123456789abcdef";
        }

        @Override
        public <T> T getSecret(String secretName, Class<T> cls) {
            throw new UnsupportedOperationException();
        }
    }
}
