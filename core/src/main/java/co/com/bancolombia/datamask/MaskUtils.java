package co.com.bancolombia.datamask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class MaskUtils {

    private MaskUtils() {
    }

    public static String mask(String fieldValue) {
        return MaskUtils.mask(fieldValue, 0,0);
    }

    public static String mask(String fieldValue, int showFirstDigitCount, int showLastDigitCount) {
        if (StringUtils.isBlank(fieldValue))
            return fieldValue;

        var leftCount = cleanIntParam(showFirstDigitCount);
        var rightCount = cleanIntParam(showLastDigitCount);
        int length = fieldValue.length();
        int shows = leftCount + rightCount;
        int hidden = length-shows;
        if(shows >= length || (leftCount == 0 && rightCount == 0)){
            return StringUtils.repeat("*",length);
        }
        return StringUtils.overlay(fieldValue,StringUtils.repeat("*",hidden) ,leftCount, leftCount + hidden);
    }

    public static String maskAsEmail(String fieldValue) {
        return maskAsEmail(fieldValue, 2, 2);
    }

    public static String maskAsEmail(String fieldValue, int leftVisible, int rightVisible) {
        String[] parts = fieldValue.split("@");

        var leftCount = 2;
        var rightCount = 1;

        String leftPart = StringUtils.left(parts[0], leftVisible);
        String rightPart = StringUtils.right(parts[0], rightVisible);
        String maskedPart = StringUtils.repeat("*", parts[0].length() - (leftCount + rightCount));

        return leftPart + maskedPart + rightPart + "@" + parts[1];
    }

    private static int cleanIntParam(int input) {
        if (input < 0)
            return 0;
        else
            return input;
    }

    public static String[] split(String input) {
        return input.replace(DataMaskingConstants.MASKING_PREFIX,"")
                .split("\\|");
    }

    public static boolean isEncryptedObject(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.OBJECT))
                .map(n -> n.findValue(DataMaskingConstants.MASKING_ATTR) != null
                        && n.findValue(DataMaskingConstants.ENCRYPTED_ATTR) !=null)
                .orElse(false);
    }
}
