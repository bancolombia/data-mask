package co.com.bancolombia.datamask;

import org.apache.commons.lang3.StringUtils;

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
        if (leftCount == 0 && rightCount == 0) {
            rightCount = 4;
        }

        String leftPart = StringUtils.left(fieldValue, leftCount);
        String maskedPart = StringUtils.repeat("*", fieldValue.length() - (leftCount + rightCount));
        String rightPart = StringUtils.right(fieldValue, rightCount);

        return leftPart + maskedPart + rightPart;
    }

    public static String maskAsEmail(String fieldValue) {
        String[] parts = fieldValue.split("@");

        var leftCount = 2;
        var rightCount = 1;

        String leftPart = StringUtils.left(parts[0], 2);
        String rightPart = StringUtils.right(parts[0], 1);
        String maskedPart = StringUtils.repeat("*", parts[0].length() - (leftCount + rightCount));

        return leftPart + maskedPart + rightPart + "@" + parts[1];
    }

    private static int cleanIntParam(int input) {
        if (input < 0)
            return 0;
        else
            return input;
    }
}
