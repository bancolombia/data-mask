package co.com.bancolombia.datamask.aws;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring Boot auto-configuration that imports {@link AwsConfiguration} when {@code secrets.dataMaskKey} is set, to any
 * value, {@code false} included.
 * <p>
 * The property condition lives here, and not in {@link AwsConfiguration}, so that an application that imports
 * {@link AwsConfiguration} directly still fails at startup when {@code secrets.dataMaskKey} is missing.
 * <p>
 * It is listed in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}, and has no
 * stereotype annotation, not even {@code @AutoConfiguration}, which is a {@code @Configuration}: otherwise an
 * application that scans {@code co.com.bancolombia} without the exclude filter of Spring Boot would process it as one
 * of its own configurations, before the application beans that {@code @ConditionalOnMissingBean} must see.
 */
@Conditional(AwsAutoConfiguration.DataMaskKeyCondition.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Import(AwsConfiguration.class)
public class AwsAutoConfiguration {

    static final String DATA_MASK_KEY = "secrets.dataMaskKey";

    /**
     * Matches when {@code secrets.dataMaskKey} is present, whatever its value: unlike {@code @ConditionalOnProperty},
     * it doesn't skip the value {@code false}, and a blank value reaches the check of {@link AwsConfiguration}. As a
     * {@link SpringBootCondition}, its outcome is in the condition evaluation report, so a missing key shows up there.
     */
    static class DataMaskKeyCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            var message = ConditionMessage.forCondition("data-mask-aws key");
            return context.getEnvironment().containsProperty(DATA_MASK_KEY)
                    ? ConditionOutcome.match(message.found("property").items(DATA_MASK_KEY))
                    : ConditionOutcome.noMatch(message.didNotFind("property").items(DATA_MASK_KEY));
        }
    }
}
