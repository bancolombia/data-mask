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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unless a test says otherwise, beans are initialized lazily, so the context starts without calling AWS Secrets
 * Manager.
 */
class AwsAutoConfigurationTests {

    private static final String REGION = "adapters.aws.secrets-manager.region=us-east-1";
    private static final String DATA_MASK_KEY = "secrets.dataMaskKey=data-mask-key";

    private final ApplicationContextRunner lazyRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.addBeanFactoryPostProcessor(
                    new LazyInitializationBeanFactoryPostProcessor()))
            .withPropertyValues(REGION);

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
    void shouldReportTheMissingDataMaskKeyInTheConditionEvaluationReport() {
        contextRunner.run(context -> assertThat(ConditionEvaluationReport.get(context.getBeanFactory())
                .getConditionAndOutcomesBySource().get(AwsAutoConfiguration.class.getName()))
                .singleElement()
                .satisfies(conditionAndOutcome -> {
                    assertThat(conditionAndOutcome.getOutcome().isMatch()).isFalse();
                    assertThat(conditionAndOutcome.getOutcome().getMessage()).contains("secrets.dataMaskKey");
                }));
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
                    assertThat(context.getBeanFactory().containsSingleton("dataMaskEncryptionKey")).isFalse();
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
    void shouldPreferTheApplicationCipherWhenImportedBeforeIt() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ImportingConfiguration.class, CustomCipherConfiguration.class)
                .run(AwsAutoConfigurationTests::assertOnlyCustomCiphers);
    }

    @Test
    void shouldPreferTheApplicationCipherWhenImportedAfterIt() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(CustomCipherConfiguration.class, ImportingConfiguration.class)
                .run(AwsAutoConfigurationTests::assertOnlyCustomCiphers);
    }

    @Test
    void shouldPreferTheApplicationCipherWhenImportedNextToIt() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ImportingNextToCustomCipherConfiguration.class)
                .run(AwsAutoConfigurationTests::assertOnlyCustomCiphers);
    }

    @Test
    void shouldPreferTheApplicationCipherWithoutAutoConfiguration() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.addBeanFactoryPostProcessor(
                        new LazyInitializationBeanFactoryPostProcessor()))
                .withPropertyValues(REGION, DATA_MASK_KEY)
                .withUserConfiguration(ImportingNextToCustomCipherConfiguration.class)
                .run(AwsAutoConfigurationTests::assertOnlyCustomCiphers);
    }

    @Test
    void shouldYieldToTheCiphersOfALaterAutoConfiguration() {
        // @ImportAutoConfiguration goes through a DeferredImportSelector, like @EnableAutoConfiguration, unlike
        // ApplicationContextRunner#withConfiguration, which registers auto-configurations as root classes.
        lazyRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(AutoConfiguredWithLaterCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("otherCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("otherDecipher");
                });
    }

    @Test
    void shouldYieldToTheCiphersOfAnAutoConfigurationWhenImported() {
        lazyRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ImportingNextToAutoConfiguredCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("otherCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("otherDecipher");
                });
    }

    @Test
    void shouldRegisterCipherBeansWithRealAutoConfiguration() {
        lazyRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(CustomCipherConfiguration.class, AutoConfiguredConfiguration.class)
                .run(AwsAutoConfigurationTests::assertOnlyCustomCiphers);
    }

    private static void assertOnlyCustomCiphers(AssertableApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
        assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("customDecipher");
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
        // Also without the exclude filter of Spring Boot: AwsConfiguration has no stereotype annotation.
        contextRunner.withUserConfiguration(PlainScanningConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(DataCipher.class));
    }

    @Test
    void shouldKeepAwsAutoConfigurationOutOfComponentScanningWithoutTheExcludeFilter() {
        // Scanned as an application configuration, it would register the AWS ciphers before the application ones.
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(PlainScanningConfiguration.class, CustomCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("customDecipher");
                });
    }

    @Test
    void shouldRegisterCipherBeansWhenDataMaskKeyIsFalse() {
        contextRunner.withPropertyValues("secrets.dataMaskKey=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("dataCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("dataDecipher");
                });
    }

    @Test
    void shouldInjectInternalBeansWithAQualifier() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(QualifiedAwsCryptoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(QualifiedAwsCryptoConsumer.class).awsCrypto())
                            .isSameAs(context.getBean("dataMaskAwsCrypto"));
                });
    }

    @Test
    void shouldNotClashWithAnApplicationBeanNamedAwsCrypto() {
        contextRunner.withPropertyValues(DATA_MASK_KEY)
                .withUserConfiguration(ApplicationAwsCryptoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AwsCrypto.class)).isSameAs(context.getBean("awsCrypto"));
                });
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
                    AwsCrypto libraryCrypto = context.getBean("dataMaskAwsCrypto", AwsCrypto.class);
                    assertThat(libraryCrypto.getEncryptionAlgorithm())
                            .isEqualTo(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA256);
                    assertThat(context.getBean(AwsCrypto.class)).isSameAs(context.getBean("awsCrypto"));
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
    @ComponentScan(basePackages = "co.com.bancolombia.datamask.aws")
    static class PlainScanningConfiguration {
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
        AwsCrypto awsCrypto() {
            return AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AwsConfiguration.class)
    static class ImportingConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Import({AwsConfiguration.class, CustomCipherConfiguration.class})
    static class ImportingNextToCustomCipherConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class, AwsAutoConfiguration.class})
    static class AutoConfiguredConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class, AwsAutoConfiguration.class,
            LateCipherAutoConfiguration.class})
    static class AutoConfiguredWithLaterCipherConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AwsConfiguration.class)
    @ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class, OtherCipherAutoConfiguration.class})
    static class ImportingNextToAutoConfiguredCipherConfiguration {
    }

    /**
     * Another auto-configuration with ciphers, ordered after the default order of auto-configurations.
     */
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE - 10)
    @ConditionalOnMissingBean({DataCipher.class, DataDecipher.class})
    static class LateCipherAutoConfiguration extends OtherCipherAutoConfiguration {
    }

    @ConditionalOnMissingBean({DataCipher.class, DataDecipher.class})
    static class OtherCipherAutoConfiguration {
        @Bean
        DataCipher otherCipher() {
            return new NoOpCipher();
        }

        @Bean
        DataDecipher otherDecipher() {
            return new NoOpDecipher();
        }
    }

    record ApplicationBeansConsumer(GenericManager manager, SecretKey secretKey) {
    }

    record QualifiedAwsCryptoConsumer(AwsCrypto awsCrypto) {
    }

    @Configuration(proxyBeanMethods = false)
    static class QualifiedAwsCryptoConfiguration {
        @Bean
        QualifiedAwsCryptoConsumer qualifiedAwsCryptoConsumer(@Qualifier("dataMaskAwsCrypto") AwsCrypto awsCrypto) {
            return new QualifiedAwsCryptoConsumer(awsCrypto);
        }
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
