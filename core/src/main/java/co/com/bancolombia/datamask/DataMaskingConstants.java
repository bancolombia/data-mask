package co.com.bancolombia.datamask;

public class DataMaskingConstants {

    private DataMaskingConstants() {
        //this is an utility class
    }

    public static final String MASKING_PREFIX = "masked_pair=";
    public static final String MASKING_ATTR = "masked";
    public static final String ENCRYPTED_ATTR = "enc";

    public static final String ENCRYPTION_INLINE = "enc_string";
    public static final String ENCRYPTION_AS_OBJECT = "enc_object";
}
