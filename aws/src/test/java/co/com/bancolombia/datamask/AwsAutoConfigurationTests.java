package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.aws.AwsConfiguration;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import com.amazonaws.encryptionsdk.AwsCrypto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Beans are initialized lazily, so the context starts without calling AWS Secrets Manager.
 */
class AwsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AwsConfiguration.class))
            .withInitializer(context -> context.addBeanFactoryPostProcessor(
                    new LazyInitializationBeanFactoryPostProcessor()))
            .withPropertyValues("adapters.aws.secrets-manager.region=us-east-1");

    @Test
    void shouldBeListedAsAutoConfiguration() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(AwsConfiguration.class.getName());
    }

    @Test
    void shouldRegisterCipherBeansWhenDataMaskKeyIsSet() {
        contextRunner.withPropertyValues("secrets.dataMaskKey=data-mask-key")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AwsCrypto.class);
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
    void shouldKeepCipherBeansDefinedByTheApplication() {
        contextRunner.withPropertyValues("secrets.dataMaskKey=data-mask-key")
                .withUserConfiguration(CustomCipherConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(DataCipher.class)).containsExactly("customCipher");
                    assertThat(context.getBeanNamesForType(DataDecipher.class)).containsExactly("customDecipher");
                    assertThat(context.getBean(DataCipher.class)).isInstanceOf(NoOpCipher.class);
                    assertThat(context.getBean(DataDecipher.class)).isInstanceOf(NoOpDecipher.class);
                });
    }

    @Test
    void shouldRegisterBeansOnceWhenAlsoImported() {
        contextRunner.withPropertyValues("secrets.dataMaskKey=data-mask-key")
                .withUserConfiguration(ImportingConfiguration.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(DataCipher.class)
                        .hasSingleBean(DataDecipher.class));
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
    @Import(AwsConfiguration.class)
    static class ImportingConfiguration {
    }
}
