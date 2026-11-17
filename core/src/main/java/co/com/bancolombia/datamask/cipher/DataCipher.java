package co.com.bancolombia.datamask.cipher;

/**
 * Encrypts the values that are masked with {@code TransformationType.ALL} or {@code TransformationType.ONLY_CIPHER}.
 *
 * <p>Implementations must be thread-safe. A single instance is shared by every serializer registered in a
 * {@code MaskingObjectMapper} or in a {@code JsonSerializer}, and the mapper calls {@link #cipher(String)}
 * concurrently from every thread that serializes with it.</p>
 */
@FunctionalInterface
public interface DataCipher {
    String cipher(String inputData);
}
