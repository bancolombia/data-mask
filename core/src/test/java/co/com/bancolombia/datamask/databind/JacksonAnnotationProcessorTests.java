package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.mask.MaskAnnotationIntrospector;
import co.com.bancolombia.datamask.databind.unmask.StringDeserializer;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonAnnotationProcessorTests {

    private static final String EMAIL = "jhon.doe12@somedomain.com";
    private static final String MASKED_EMAIL = DataMaskingConstants.MASKING_PREFIX
            + "jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ==";
    private static final String MASKED_EMAIL_JSON = "{\"name\":\"Jhon Doe\",\"email\":\"" + MASKED_EMAIL + "\"}";

    @Test
    void testMaskReadOnly() {
        JsonMapper mapper = new MaskingObjectMapper();

        Person p = new Person("Jhon Doe", "4444555566667777");

        String jsonValue = mapper.writeValueAsString(p);
        System.out.println(jsonValue);
        assertTrue(jsonValue.contains("************7777"));
        assertFalse(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX));
    }

    @Test
    void testMaskingInlineFormat() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        Customer c = new Customer("Jhon Doe", "jhon.doe12@somedomain.com");

        String jsonValue = mapper.writeValueAsString(c);
        System.out.println(jsonValue);
        assertTrue(jsonValue.contains("masked_pair=jh*******2@somedomain" +
                ".com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ=="));
    }

    @Test
    void testUnmaskingInlineFormat() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        Customer customer = mapper.readValue(MASKED_EMAIL_JSON, Customer.class);
        System.out.println(customer.toString());
        assertEquals("Jhon Doe", customer.getName());
        assertEquals(EMAIL, customer.getEmail());
    }

    @Test
    void testMaskingAsObject() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        Company c = new Company("Acme enterprises", "9999888877776666");

        String jsonValue = mapper.writeValueAsString(c);
        System.out.println(jsonValue);
        assertTrue(jsonValue.contains("\"masked\":\"999*********6666\""));
        assertTrue(jsonValue.contains("\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\""));
    }

    @Test
    void testUnmaskingFromObject() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Acme enterprises\",\"card\":{\"masked\":\"999*********6666\"," +
                "\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\"}}";

        Company company = mapper.readValue(json, Company.class);
        System.out.println(company.toString());
        assertEquals("Acme enterprises", company.getName());
        assertEquals("9999888877776666", company.getCard());
    }

    @Test
    void testUnmaskingAsObjecMapperModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StringDeserializer(new DummyDecipher()));
        JsonMapper mapper = JsonMapper.builder().addModule(module).build();

        Customer customer = mapper.readValue(MASKED_EMAIL_JSON, Customer.class);
        System.out.println(customer.toString());
        assertEquals("Jhon Doe", customer.getName());
        assertEquals(EMAIL, customer.getEmail());

    }

    @Test
    void testMaskOnGetterAppliesToBothDirections() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String jsonValue = mapper.writeValueAsString(new GetterAnnotated("Jhon Doe", "4444555566667777"));
        assertTrue(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX + "************7777|"));
        assertFalse(jsonValue.contains("4444555566667777"));

        GetterAnnotated read = mapper.readValue(jsonValue, GetterAnnotated.class);
        assertEquals("4444555566667777", read.getCard());
    }

    @Test
    void testMaskOnSetterAppliesToBothDirections() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        SetterAnnotated read = mapper.readValue(MASKED_EMAIL_JSON, SetterAnnotated.class);
        assertEquals(EMAIL, read.getEmail());

        String jsonValue = mapper.writeValueAsString(read);
        assertTrue(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX));
        assertFalse(jsonValue.contains(EMAIL));
    }

    @Test
    void testBareMaskOnSetterMasksWithoutDeciphering() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        BareSetterAnnotated read = mapper.readValue(MASKED_EMAIL_JSON, BareSetterAnnotated.class);
        assertEquals(MASKED_EMAIL, read.getEmail());

        String jsonValue = mapper.writeValueAsString(new BareSetterAnnotated("Jhon Doe", EMAIL));
        assertTrue(jsonValue.contains("\"email\":\"" + "*".repeat(EMAIL.length()) + "\""));
    }

    @Test
    void testMaskOnRecordComponent() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String jsonValue = mapper.writeValueAsString(new CardRecord("Jhon Doe", "4444555566667777"));
        assertTrue(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX + "************7777|"));
        assertEquals("4444555566667777", mapper.readValue(jsonValue, CardRecord.class).card());
    }

    @Test
    void testMaskOnNonStringGetterIsRejected() {
        JsonMapper mapper = new MaskingObjectMapper();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new NonStringGetter(1234)));
        assertTrue(e.getMessage().contains("@Mask supports String members only"));
    }

    @Test
    void testMaskOnNonStringSetterIsRejected() {
        JsonMapper mapper = new MaskingObjectMapper();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.readValue("{\"age\":30}", NonStringSetter.class));
        assertTrue(e.getMessage().contains("@Mask supports String members only"));
    }

    @Test
    void testMaskOnHelperMethodIsRejected() {
        JsonMapper mapper = new MaskingObjectMapper();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new HelperAnnotated("4444555566667777")));
        assertTrue(e.getMessage().contains("maskCard"));
    }

    @Test
    void testMaskOnFluentAccessorIsRejected() {
        JsonMapper mapper = new MaskingObjectMapper();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new FluentAnnotated("4444555566667777")));
        assertTrue(e.getMessage().contains("has no effect"));
    }

    @Test
    void testAccessorModuleOnCustomMapper() {
        JsonMapper mapper = JsonMapper.builder()
                .annotationIntrospector(AnnotationIntrospector.pair(
                        new MaskAnnotationIntrospector(new DummyCipher()), new JacksonAnnotationIntrospector()))
                .addModule(new MaskAccessorModule())
                .build();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new HelperAnnotated("4444555566667777")));
        assertTrue(e.getMessage().contains("maskCard"));
    }

    @Test
    void testSetterOnlyPropertyIsAccepted() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        SetterOnlyAnnotated read = mapper.readValue(MASKED_EMAIL_JSON, SetterOnlyAnnotated.class);
        assertEquals(EMAIL, read.email);
        assertEquals("{\"name\":\"Jhon Doe\"}", mapper.writeValueAsString(read));
    }

    @Test
    void testGetterOnlyPropertyIsAccepted() {
        JsonMapper mapper = new MaskingObjectMapper();

        String jsonValue = mapper.writeValueAsString(new GetterOnlyAnnotated("Jhon Doe", "4444555566667777"));
        assertTrue(jsonValue.contains("\"card\":\"************7777\""));
        assertEquals("Jhon Doe", mapper.readValue(jsonValue, GetterOnlyAnnotated.class).name);
    }

    @Test
    void testWriteOnlyMaskedField() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        assertEquals("{\"name\":\"Jhon Doe\"}", mapper.writeValueAsString(new WriteOnlyAnnotated("Jhon Doe", EMAIL)));

        String json = "{\"name\":\"Jhon Doe\",\"password\":\"" + MASKED_EMAIL + "\"}";
        assertEquals(EMAIL, mapper.readValue(json, WriteOnlyAnnotated.class).getPassword());
    }

    @Test
    void testOnlyCipherRoundTrip() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String jsonValue = mapper.writeValueAsString(new CipherOnly("Jhon Doe", "4444555566667777"));
        assertTrue(jsonValue.contains("\"card\":\"NDQ0NDU1NTU2NjY2Nzc3Nw==\""));
        assertEquals("4444555566667777", mapper.readValue(jsonValue, CipherOnly.class).getCard());
    }

    @Test
    void testMalformedMaskedValueIsAnInputMismatch() {
        JsonMapper mapper = new MaskingObjectMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Jhon Doe\",\"email\":\"" + DataMaskingConstants.MASKING_PREFIX + "clientvalue\"}";
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> mapper.readValue(json, Customer.class));
        assertTrue(e.getMessage().contains("<masked value>|<encrypted value>"));
    }

    @Test
    void testNoMasking() {
        JsonMapper mapper = new MaskingObjectMapper();
        Person p = new Person("Jhon Doe", "");
        String jsonValue = mapper.writeValueAsString(p);
        System.out.println(jsonValue);
        assertFalse(jsonValue.contains("*"));
    }

    @Test
    void testJsonIncludeNonNullIsHonored() {
        JsonMapper mapper = new MaskingObjectMapper();
        Account account = new Account("Jhon Doe", null, "4444555566667777");
        String jsonValue = mapper.writeValueAsString(account);
        System.out.println(jsonValue);
        assertFalse(jsonValue.contains("alias"));
        assertTrue(jsonValue.contains("************7777"));
    }

    @Test
    void testJsonPropertyIsHonored() {
        JsonMapper mapper = new MaskingObjectMapper();
        Account account = new Account("Jhon Doe", "JD", "4444555566667777");
        String jsonValue = mapper.writeValueAsString(account);
        System.out.println(jsonValue);
        assertTrue(jsonValue.contains("\"full_name\":\"Jhon Doe\""));
    }

    @Test
    void testJsonIgnoreIsHonored() {
        JsonMapper mapper = new MaskingObjectMapper();
        Account account = new Account("Jhon Doe", "JD", "4444555566667777");
        String jsonValue = mapper.writeValueAsString(account);
        System.out.println(jsonValue);
        assertFalse(jsonValue.contains("alias"));
        assertFalse(jsonValue.contains("JD"));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Person {
        private String name;

        @Mask(rightVisible = 4)
        private String card;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private String name;

        @Mask(queryOnly = TransformationType.ALL, isEmail = true, leftVisible = 2, rightVisible = 1)
        private String email;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Company {
        private String name;

        @Mask(leftVisible = 3, rightVisible = 4, queryOnly = TransformationType.ALL, format =
                DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String card;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Account {
        @JsonProperty("full_name")
        private String name;

        @JsonIgnore
        private String alias;

        @Mask(rightVisible = 4)
        private String card;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Client {
        private String name;

        @Mask(rightVisible = 4, queryOnly = TransformationType.ALL)
        private String[] card;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GetterAnnotated {
        private String name;
        private String card;

        @Mask(rightVisible = 4, queryOnly = TransformationType.ALL)
        public String getCard() {
            return card;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SetterAnnotated {
        private String name;
        private String email;

        @Mask(queryOnly = TransformationType.ALL)
        public void setEmail(String email) {
            this.email = email;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BareSetterAnnotated {
        private String name;
        private String email;

        @Mask
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public record CardRecord(String name, @Mask(rightVisible = 4, queryOnly = TransformationType.ALL) String card) {
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NonStringGetter {
        private int pin;

        @Mask(rightVisible = 4)
        public int getPin() {
            return pin;
        }
    }

    @Data
    @NoArgsConstructor
    public static class NonStringSetter {
        private int age;

        @Mask(queryOnly = TransformationType.ALL)
        public void setAge(int age) {
            this.age = age;
        }
    }

    @AllArgsConstructor
    public static class HelperAnnotated {
        public String card;

        @Mask(rightVisible = 4)
        public String maskCard() {
            return card;
        }
    }

    @AllArgsConstructor
    public static class FluentAnnotated {
        private String card;

        @Mask(rightVisible = 4)
        public String card() {
            return card;
        }
    }

    public static class SetterOnlyAnnotated {
        public String name;
        private String email;

        @Mask(queryOnly = TransformationType.ALL)
        public void setEmail(String email) {
            this.email = email;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class GetterOnlyAnnotated {
        public String name;
        private String card;

        @Mask(rightVisible = 4)
        public String getCard() {
            return card;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WriteOnlyAnnotated {
        private String name;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Mask(queryOnly = TransformationType.ALL)
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CipherOnly {
        private String name;

        @Mask(queryOnly = TransformationType.ONLY_CIPHER)
        private String card;
    }

    public static class DummyCipher implements DataCipher {
        @Override
        public String cipher(String inputData) {
            return Base64.getEncoder().encodeToString(inputData.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class DummyDecipher implements DataDecipher {
        @Override
        public String decipher(String inputData) {
            return new String(Base64.getDecoder().decode(inputData.getBytes(StandardCharsets.UTF_8)));
        }
    }

}
