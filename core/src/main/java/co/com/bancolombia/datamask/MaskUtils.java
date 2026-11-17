package co.com.bancolombia.datamask;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;

import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MaskUtils {

    public static final String INLINE_FORMAT_EXCEPTION = "Expected " + DataMaskingConstants.MASKING_PREFIX
            + "<masked value>" + DataMaskingConstants.MASKING_SEPARATOR + "<encrypted value>";

    public static String mask(String fieldValue) {
        return MaskUtils.mask(fieldValue, 0, 0);
    }

    public static String mask(String fieldValue, int showFirstDigitCount, int showLastDigitCount) {
        return MaskUtils.mask(fieldValue, showFirstDigitCount, showLastDigitCount, false, null);
    }

    public static String mask(String fieldValue, int showFirstDigitCount, int showLastDigitCount, boolean isMultiMask,
                              String separator) {
        if (StringUtils.isBlank(fieldValue))
            return fieldValue;

        var leftCount = cleanIntParam(showFirstDigitCount);
        var rightCount = cleanIntParam(showLastDigitCount);
        int length = fieldValue.length();
        int shows = leftCount + rightCount;
        int hidden = length - shows;
        if (shows >= length || (leftCount == 0 && rightCount == 0)) {
            return StringUtils.repeat("*", length);
        }
        if (isMultiMask && StringUtils.isNotEmpty(separator) && fieldValue.contains(separator)) {
            String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(fieldValue, separator);
            for (int i = 0; i < parts.length; i++) {
                parts[i] = mask(parts[i], showFirstDigitCount, showLastDigitCount, false, null);
            }
            return String.join(separator, parts);
        }
        return StringUtils.overlay(fieldValue, StringUtils.repeat("*", hidden), leftCount, leftCount + hidden);
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
        return Math.max(input, 0);
    }

    /**
     * Splits an inline value ({@code masked_pair=<masked value>|<encrypted value>}) into the masked value and the
     * encrypted value. The prefix is removed only at the start, and the value is split on the <b>last</b> {@code |}:
     * the masked value comes from the original data and may contain {@code |}, while the encrypted value must not.
     *
     * @return the masked value and the encrypted value, or a single element when the input has no {@code |}.
     */
    public static String[] split(String input) {
        String value = Strings.CS.removeStart(input, DataMaskingConstants.MASKING_PREFIX);
        int separatorIndex = value.lastIndexOf(DataMaskingConstants.MASKING_SEPARATOR);
        if (separatorIndex < 0) {
            return new String[]{value};
        }
        return new String[]{value.substring(0, separatorIndex),
                value.substring(separatorIndex + DataMaskingConstants.MASKING_SEPARATOR.length())};
    }

    /**
     * Returns the encrypted value of an inline value ({@code masked_pair=<masked value>|<encrypted value>}), or an
     * empty {@link Optional} when the input has no {@code |} or its encrypted value is empty.
     */
    public static Optional<String> findEncryptedValue(String input) {
        String[] parts = split(input);
        return parts.length == 2 && StringUtils.isNotEmpty(parts[1]) ? Optional.of(parts[1]) : Optional.empty();
    }

    public static boolean isEncryptedObject(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.OBJECT))
                .map(n -> n.findValue(DataMaskingConstants.MASKING_ATTR) != null
                        && n.findValue(DataMaskingConstants.ENCRYPTED_ATTR) != null)
                .orElse(false);
    }
}
