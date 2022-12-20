package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import co.com.bancolombia.datamask.databind.mask.*;
import co.com.bancolombia.datamask.databind.unmask.DataUnmasked;
import co.com.bancolombia.datamask.databind.unmask.JsonDeserializer;
import co.com.bancolombia.datamask.databind.util.QueryType;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JacksonJsonProcessorTests {

    private ObjectMapper mapper;

    @BeforeEach
    void init() {
        mapper = new ObjectMapper();
    }

    private void setCipherMapper(DataCipher dataCipher, DataDecipher dataDecipher) {
        var module = new SimpleModule();
        module.addSerializer(DataMask.class, new JsonSerializer(DataMask.class, dataCipher));
        module.addSerializer(DataUnmasked.class, new JsonDeserializer(DataUnmasked.class, dataDecipher));
        mapper.registerModule(module);
    }

    @Test
    void testMaskReadOnly() {
        setCipherMapper(new NoOpCipher(), new NoOpDecipher());
        String json = "{\"name\":\"EllaCruickshank\",\"mail\":\"Glennie42@yahoo.com\",\"password\":\"DGiZmwE5VFVpbiL\"}";
        var identifyFieldName = new IdentifyField("name", QueryType.NAME);
        var identifyFieldPass = new IdentifyField("password", QueryType.NAME);
        var identifyFieldMail = new IdentifyField("mail", QueryType.NAME);

        var defaultFields = List.of(identifyFieldName, identifyFieldPass, identifyFieldMail);

        try {
            String jsonValue = mapper.writeValueAsString(new DataMask<>(json, defaultFields));
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("**************"));
            assertFalse(jsonValue.contains(DataMaskingConstants.MASKING_PREFIX));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testMaskingInlineFormat() {
        setCipherMapper(new DummyCipher(), new NoOpDecipher());
        Customer c = new Customer("Jhon Doe", "jhon.doe12@somedomain.com");
        var identifyFieldEmail = new IdentifyField("email", QueryType.NAME);
        var maskFormat = Map.of(identifyFieldEmail, new MaskingFormat(2, 1, true, TransformationType.ALL));

        try {
            String jsonValue = mapper.writeValueAsString(new DataMask<>(c, maskFormat));
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("masked_pair=jh*******2@somedomain.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ=="));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testUnmaskingInlineFormat() {
        setCipherMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Jhon Doe\",\"email\":\"masked_pair=jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ==\"}";
        var fieldsMasked = List.of("email");
        var fields = Map.of(
                new IdentifyField("/email", QueryType.PATH), new MaskingFormat(1, 0, true, TransformationType.ALL));
        try {
            Customer customer = mapper.convertValue(new DataUnmasked(json, fields), Customer.class);
            System.out.println(customer.toString());
            assertEquals("Jhon Doe", customer.getName());
            assertEquals("jhon.doe12@somedomain.com", customer.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testMaskingAsObject() {
        setCipherMapper(new DummyCipher(), new DummyDecipher());

        Company c = new Company("Acme enterprises", "9999888877776666");
        var identifyFieldCard = new IdentifyField("card", QueryType.NAME);
        var maskFormat = Map.of(identifyFieldCard, new MaskingFormat(3, 4, false, TransformationType.ALL, DataMaskingConstants.ENCRYPTION_AS_OBJECT));

        try {
            String jsonValue = mapper.writeValueAsString(new DataMask<>(c, maskFormat));
            System.out.println(jsonValue);
            assertTrue(jsonValue.contains("\"masked\":\"999*********6666\""));
            assertTrue(jsonValue.contains("\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\""));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    void testUnmaskingFromObject() {
        setCipherMapper(new DummyCipher(), new DummyDecipher());
        String json = "{\"name\":\"Acme enterprises\",\"card\":{\"masked\":\"999*********6666\",\"enc\":\"OTk5OTg4ODg3Nzc3NjY2Ng==\"}}";
        var maskFormat = List.of("card");

        Company company = mapper.convertValue(new DataUnmasked(json, Map.of(new IdentifyField("card", QueryType.NAME), new MaskingFormat(TransformationType.ALL))), Company.class);
        System.out.println(company.toString());
        assertEquals("Acme enterprises", company.getName());
        assertEquals("9999888877776666", company.getCard());
    }

    @Test
    void testUnmaskingAsObjecMapperModule() {
        setCipherMapper(new DummyCipher(), new DummyDecipher());

        String json = "{\"name\":\"Jhon Doe\",\"email\":\"masked_pair=jho******************.com|amhvbi5kb2UxMkBzb21lZG9tYWluLmNvbQ==\"}";
        var fieldsMasked = List.of("email");
        Customer customer = mapper.convertValue(new DataUnmasked(json, Map.of(new IdentifyField("email", QueryType.NAME), new MaskingFormat(0,0, true, TransformationType.ALL))), Customer.class);
        System.out.println(customer.toString());
        assertEquals("Jhon Doe", customer.getName());
        assertEquals("jhon.doe12@somedomain.com", customer.getEmail());
    }

    @Test
    void testNoMasking() {
        setCipherMapper(new NoOpCipher(), new NoOpDecipher());
        Person p = new Person("Jhon Doe", "");
        var masking = new MaskingFormat();
        masking.setRightVisible(4);
        var identifyFieldCard = new IdentifyField("card", QueryType.NAME);


        var fieldsMasked = Map.of(identifyFieldCard, masking);
        try {
            String jsonValue = mapper.writeValueAsString(new DataMask<>(p, fieldsMasked));
            System.out.println(jsonValue);
            assertFalse(jsonValue.contains("*"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }


    @Test
    void testMaskingBigJson() throws JsonProcessingException {
        setCipherMapper(new DummyCipher(), new DummyDecipher());
        String json = "[{\"_id\":\"6346bb6e33d2654ae8d99356\",\"index\":0,\"guid\":\"14fe161d-6704-48a0-8b08-b3a3b26a5722\",\"isActive\":true,\"balance\":\"$1,492.00\",\"picture\":\"http://placehold.it/32x32\",\"age\":21,\"eyeColor\":\"brown\",\"name\":\"CarolinaDavis\",\"gender\":\"female\",\"company\":\"BLUPLANET\",\"email\":\"carolinadavis@bluplanet.com\",\"phone\":\"+1(808)525-2388\",\"address\":\"599BeaumontStreet,Albany,Alabama,931\",\"about\":\"Nisilaborenostrudfugiattemporeatemporquiscupidatatullamcoreprehenderitsitsuntad.Occaecatdolordoloreminimproidentcommodoidminim.Commodominimidoccaecatadipisicingeualiquain.\\r\\n\",\"registered\":\"2022-03-21T09:28:40+05:00\",\"latitude\":-31.105209,\"longitude\":131.291873,\"tags\":[\"cillum\",\"tempor\",\"cupidatat\",\"ullamco\",\"amet\",\"aliquip\",\"laborum\"],\"friends\":[{\"id\":0,\"name\":\"BerryHowell\"},{\"id\":1,\"name\":\"CummingsBecker\"},{\"id\":2,\"name\":\"RandiGriffin\"}],\"greeting\":\"Hello,CarolinaDavis!Youhave6unreadmessages.\",\"favoriteFruit\":\"banana\"}]";

        var identifyFieldId = new IdentifyField("_id", QueryType.NAME);
        var identifyFieldGuid = new IdentifyField("guid", QueryType.NAME);
        var identifyFieldEmail = new IdentifyField("email", QueryType.NAME);
        var identifyFieldName = new IdentifyField("name", QueryType.NAME);
        var identifyFieldNameInArray = new IdentifyField("/friends/0/name", QueryType.PATH);


        var fields = Map.of(identifyFieldId, new MaskingFormat(),
                identifyFieldGuid, new MaskingFormat(4, 4, false, TransformationType.ALL),
                identifyFieldEmail, new MaskingFormat(0, 0, true, TransformationType.ONLY_MASK),
                identifyFieldName, new MaskingFormat(0, 2),
                identifyFieldNameInArray, new MaskingFormat());
        //Mask and cipher
        String jsonValue = mapper.writeValueAsString(new DataMask<>(json, fields));
        System.out.println(jsonValue);

        assertTrue(jsonValue.contains("\"_id\":\"************************\""));
        assertTrue(jsonValue.contains("\"email\":\"**********@bluplanet.com\""));
        assertTrue(jsonValue.contains("\"name\":\"*********"));
        assertTrue(jsonValue.contains("\"guid\":\"masked_pair=14fe****************************5722|MTRmZTE2MWQtNjcwNC00OGEwLThiMDgtYjNhM2IyNmE1NzIy\""));
        jsonValue = mapper.writeValueAsString(new DataUnmasked(jsonValue, Map.of(new IdentifyField("guid", QueryType.NAME), new MaskingFormat(TransformationType.ALL))));

        System.out.println(jsonValue);
        assertTrue(jsonValue.contains("\"guid\":\"14fe161d-6704-48a0-8b08-b3a3b26a5722\""));
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
        private String email;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Company {
        private String name;
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
