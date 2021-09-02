package co.com.bancolombia.datamask.cipher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoOpCipherDecipherTests {

    private NoOpCipher cipher;
    private NoOpDecipher decipher;

    @BeforeEach
    void prepare() {
        cipher = new NoOpCipher();
        decipher = new NoOpDecipher();
    }

    @Test
    void testNoOpCipher() {
        Assertions.assertEquals("*******orld", cipher.cipher("Hello World"));
    }

    @Test
    void testNoOpDecipher() {
        Assertions.assertEquals("*******orld", decipher.decipher("*******orld"));
    }
}
