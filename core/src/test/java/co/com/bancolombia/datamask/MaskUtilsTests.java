package co.com.bancolombia.datamask;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class MaskUtilsTests {

    private final String testString = "Super Simple Example Text";

    @Test
    void testMaskDefault() {
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
        Assertions.assertEquals("jh******12@somedomain.com", masked);
    }

    @Test
    void testSplitOnLastSeparator() {
        Assertions.assertArrayEquals(new String[]{"a|b|cd****", "ZW5j"},
                MaskUtils.split("masked_pair=a|b|cd****|ZW5j"));
        Assertions.assertArrayEquals(new String[]{"*****f||gh|", "ZW5j"},
                MaskUtils.split("masked_pair=*****f||gh||ZW5j"));
    }

    @Test
    void testSplitRemovesPrefixOnlyAtStart() {
        Assertions.assertArrayEquals(new String[]{"xmasked_pair=y", "ZW5j"},
                MaskUtils.split("masked_pair=xmasked_pair=y|ZW5j"));
    }

    @Test
    void testSplitWithoutSeparatorReturnsSingleElement() {
        Assertions.assertArrayEquals(new String[]{"abc"}, MaskUtils.split("masked_pair=abc"));
    }

    @Test
    void testFindEncryptedValue() {
        Assertions.assertEquals(Optional.of("ZW5j"), MaskUtils.findEncryptedValue("masked_pair=a|b|ZW5j"));
        Assertions.assertEquals(Optional.of("ZW5j"), MaskUtils.findEncryptedValue("masked_pair=|ZW5j"));
        Assertions.assertEquals(Optional.empty(), MaskUtils.findEncryptedValue("masked_pair=abc"));
        Assertions.assertEquals(Optional.empty(), MaskUtils.findEncryptedValue("masked_pair=abc|"));
        Assertions.assertEquals(Optional.empty(), MaskUtils.findEncryptedValue("masked_pair=a||b|"));
    }

    @Test
    void testMaskAsEmailMasksShortLocalPartEntirely() {
        Assertions.assertEquals("**@bank.com", MaskUtils.maskAsEmail("ab@bank.com", 2, 1));
        Assertions.assertEquals("*@bank.com", MaskUtils.maskAsEmail("a@bank.com", 2, 1));
        Assertions.assertEquals("********@x.com", MaskUtils.maskAsEmail("john.doe@x.com", 4, 4));
    }

    @Test
    void testMaskAsEmailUsesVisibleCharacterCounts() {
        Assertions.assertEquals("jo*****e@x.com", MaskUtils.maskAsEmail("john.doe@x.com", 2, 1));
        Assertions.assertEquals("joh**doe@x.com", MaskUtils.maskAsEmail("john.doe@x.com", 3, 3));
        Assertions.assertEquals("********@x.com", MaskUtils.maskAsEmail("john.doe@x.com", 0, 0));
    }

    @Test
    void testMaskAsEmailWithoutAtFallsBackToMask() {
        Assertions.assertEquals("ab***f", MaskUtils.maskAsEmail("abcdef", 2, 1));
        Assertions.assertEquals("ab*d@", MaskUtils.maskAsEmail("abcd@", 2, 1));
    }

    @Test
    void testMaskAsEmailSplitsOnLastAt() {
        Assertions.assertEquals("\"a****\"@x.com", MaskUtils.maskAsEmail("\"a@b.c\"@x.com", 2, 1));
    }

    @Test
    void testMaskAsEmailKeepsBlankValues() {
        Assertions.assertNull(MaskUtils.maskAsEmail(null, 2, 1));
        Assertions.assertEquals("", MaskUtils.maskAsEmail("", 2, 1));
    }
}
