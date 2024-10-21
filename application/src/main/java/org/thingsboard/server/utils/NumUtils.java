package org.thingsboard.server.utils;

import java.math.BigDecimal;

public class NumUtils {


    /***
     * 获取一位
     * @return
     */
    public static double get1Double(double num) {
        return new BigDecimal(num).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();

    }

}
