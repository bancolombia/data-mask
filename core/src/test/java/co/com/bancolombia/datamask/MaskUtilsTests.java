package co.com.bancolombia.datamask;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MaskUtilsTests {

    private final String testString = "Super Simple Example Text";

    @Test
    void testMaskDefault() {
        String json = "{\n" +
                "        \"name\": \"Jhon Doe\",\n" +
                "        \"email\": \"Jho**************.com\",\n" +
                "        \"creditCardNumber\": {\n" +
                "        \"masked\": \"************5678\",\n" +
                "        \"enc\": \"dGhpcyBzaG91bGQgYmUgYW4gZWNyeXB0ZWQgdmFsdWUK\"\n" +
                "        }\n" +
                "        }";
        Assertions.assertEquals("*************************", MaskUtils.mask(testString));
        Assertions.assertEquals("*************************", MaskUtils.mask(testString,0,0));
        Assertions.assertEquals("*************************", MaskUtils.mask(testString,-1,-1));
    }

    @Test
    void testMaskLeft() {
        Assertions.assertEquals("Super********************", MaskUtils.mask(testString,5,0));
    }

    @Test
    void testMaskRight() {
        Assertions.assertEquals("*********************Text", MaskUtils.mask(testString,0,4));
    }

    @Test
    void testMaskLeftAndRight() {
        Assertions.assertEquals("Super****************Text", MaskUtils.mask(testString,5,4));
    }

    @Test
    void testMaskNullValue() {
        Assertions.assertNull(MaskUtils.mask(null, 5, 4));
    }

    @Test
    void testMultiMaskWithSpaceSeparator() {
        Assertions.assertEquals("H***o W***d", MaskUtils.mask("Hello World", 1, 1, true, " "));
    }

    @Test
    void testMultiMaskWithRegexMetacharacterSeparators() {
        Assertions.assertEquals("1**4.5**8", MaskUtils.mask("1234.5678", 1, 1, true, "."));
        Assertions.assertEquals("1**4|5**8", MaskUtils.mask("1234|5678", 1, 1, true, "|"));
        Assertions.assertEquals("1**4+5**8", MaskUtils.mask("1234+5678", 1, 1, true, "+"));
        Assertions.assertEquals("1**4*5**8", MaskUtils.mask("1234*5678", 1, 1, true, "*"));
    }

    @Test
    void testMultiMaskWithMultiCharacterSeparators() {
        Assertions.assertEquals("1**4--5**8", MaskUtils.mask("1234--5678", 1, 1, true, "--"));
        Assertions.assertEquals("J**n, M**y", MaskUtils.mask("John, Mary", 1, 1, true, ", "));
    }

    @Test
    void testMultiMaskPreservesEmptyParts() {
        Assertions.assertEquals("1**4,,5**8,", MaskUtils.mask("1234,,5678,", 1, 1, true, ","));
    }

    @Test
    void testMultiMaskWithoutSeparatorMasksWholeValue() {
        Assertions.assertEquals("H*********d", MaskUtils.mask("Hello World", 1, 1, true, null));
        Assertions.assertEquals("H*********d", MaskUtils.mask("Hello World", 1, 1, true, ""));
    }

    @Test
    void testMaskAsEmailDefaultUsesTwoVisibleCharactersOnEachSide() {
        String email = "jhon.doe12@somedomain.com";
        String masked = MaskUtils.maskAsEmail(email);

        Assertions.assertEquals(MaskUtils.maskAsEmail(email, 2, 2), masked);
        Assertions.assertTrue(masked.startsWith("jh*"));
        Assertions.assertTrue(masked.endsWith("12@somedomain.com"));
    }
}
