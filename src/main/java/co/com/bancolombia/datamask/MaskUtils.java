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

    private static int cleanIntParam(int input) {
        if (input < 0)
            return 0;
        else
            return input;
    }
}
