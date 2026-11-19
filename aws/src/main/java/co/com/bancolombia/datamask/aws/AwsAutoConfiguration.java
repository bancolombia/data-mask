package co.com.bancolombia.datamask.aws;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration that imports {@link AwsConfiguration} when {@code secrets.dataMaskKey} is set.
 * <p>
 * The property condition lives here, and not in {@link AwsConfiguration}, so that an application that imports
 * {@link AwsConfiguration} directly still fails at startup when {@code secrets.dataMaskKey} is missing.
 */
@AutoConfiguration
@ConditionalOnProperty("secrets.dataMaskKey")
@Import(AwsConfiguration.class)
public class AwsAutoConfiguration {
}
