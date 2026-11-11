package co.com.bancolombia.datamask.cipher;

/**
 * Decrypts the values that were masked with {@code TransformationType.ALL} or {@code TransformationType.ONLY_CIPHER}.
 *
 * <p>Implementations must be thread-safe. A single instance is shared by every deserializer registered in a
 * {@code MaskingObjectMapper} or in a {@code JsonDeserializer}, and the mapper calls {@link #decipher(String)}
 * concurrently from every thread that uses it.</p>
 */
@FunctionalInterface
public interface DataDecipher {
    String decipher(String inputData);
}
