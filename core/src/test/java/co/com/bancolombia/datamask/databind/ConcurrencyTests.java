package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.databind.JacksonAnnotationProcessorTests.Company;
import co.com.bancolombia.datamask.databind.JacksonAnnotationProcessorTests.Customer;
import co.com.bancolombia.datamask.databind.JacksonJsonProcessorTests.DummyCipher;
import co.com.bancolombia.datamask.databind.JacksonJsonProcessorTests.DummyDecipher;
import co.com.bancolombia.datamask.databind.mask.DataMask;
import co.com.bancolombia.datamask.databind.mask.IdentifyField;
import co.com.bancolombia.datamask.databind.mask.JsonSerializer;
import co.com.bancolombia.datamask.databind.mask.MaskingFormat;
import co.com.bancolombia.datamask.databind.unmask.DataUnmasked;
import co.com.bancolombia.datamask.databind.unmask.JsonDeserializer;
import co.com.bancolombia.datamask.databind.util.QueryType;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Serializers, deserializers and the {@code DataCipher}/{@code DataDecipher} they hold are shared by every thread that
 * uses the same mapper. These tests run many masking and unmasking operations at the same time, each one with its
 * own data, and check that no result is mixed with the data of another thread.
 */
class ConcurrencyTests {

    private static final int THREADS = 16;
    private static final int TASKS = 500;

    private static final Map<IdentifyField, MaskingFormat> FIELDS = Map.of(
            new IdentifyField("email", QueryType.NAME), new MaskingFormat(2, 1, true, TransformationType.ALL),
            new IdentifyField("/account/card", QueryType.PATH), new MaskingFormat(0, 4, false, TransformationType.ALL));

    @Test
    void shouldMaskAndUnmaskDataMaskConcurrentlyWithSharedMapper() throws Exception {
        ObjectMapper mapper = dataMaskMapper();
        List<String> expectedMasked = IntStream.range(0, TASKS)
                .mapToObj(i -> mapper.writeValueAsString(new DataMask<>(json(i), FIELDS)))
                .toList();

        List<String> masked = runConcurrently(i -> mapper.writeValueAsString(new DataMask<>(json(i), FIELDS)));
        List<JsonNode> unmasked = runConcurrently(i -> mapper.valueToTree(new DataUnmasked(masked.get(i), FIELDS)));

        for (int i = 0; i < TASKS; i++) {
            assertEquals(expectedMasked.get(i), masked.get(i));
            assertTrue(masked.get(i).contains(DataMaskingConstants.MASKING_PREFIX));
            assertFalse(masked.get(i).contains(email(i)));
            assertEquals(mapper.readTree(json(i)), unmasked.get(i));
        }
    }

    @Test
    void shouldMaskAndUnmaskAnnotatedPojosConcurrentlyWithSharedMapper() throws Exception {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        List<String> maskedCustomers = runConcurrently(i -> mapper.writeValueAsString(customer(i)));
        List<String> maskedCompanies = runConcurrently(i -> mapper.writeValueAsString(company(i)));
        List<Customer> customers = runConcurrently(i -> mapper.readValue(maskedCustomers.get(i), Customer.class));
        List<Company> companies = runConcurrently(i -> mapper.readValue(maskedCompanies.get(i), Company.class));

        for (int i = 0; i < TASKS; i++) {
            assertFalse(maskedCustomers.get(i).contains(email(i)));
            assertFalse(maskedCompanies.get(i).contains(card(i)));
            assertEquals(customer(i), customers.get(i));
            assertEquals(company(i), companies.get(i));
        }
    }

    private static ObjectMapper dataMaskMapper() {
        var module = new SimpleModule();
        @SuppressWarnings("unchecked")
        Class<DataMask<?>> dataMaskClass = (Class<DataMask<?>>) (Class<?>) DataMask.class;
        module.addSerializer(dataMaskClass, new JsonSerializer(DataMask.class, new DummyCipher()));
        module.addSerializer(DataUnmasked.class, new JsonDeserializer(DataUnmasked.class, new DummyDecipher()));
        return JsonMapper.builder().addModule(module).build();
    }

    private static <T> List<T> runConcurrently(IntFunction<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            var start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>(TASKS);
            for (int i = 0; i < TASKS; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.apply(index);
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>(TASKS);
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static String json(int i) {
        return "{\"name\":\"Customer " + i + "\",\"email\":\"" + email(i) + "\","
                + "\"account\":{\"card\":\"" + card(i) + "\"}}";
    }

    private static Customer customer(int i) {
        return new Customer("Customer " + i, email(i));
    }

    private static Company company(int i) {
        return new Company("Company " + i, card(i));
    }

    private static String email(int i) {
        return "customer" + i + "@example.com";
    }

    private static String card(int i) {
        return String.format("4500%012d", i);
    }
}
