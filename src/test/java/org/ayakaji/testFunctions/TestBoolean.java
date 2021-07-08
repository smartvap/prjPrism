package org.ayakaji.testFunctions;

import org.junit.Test;

/**
 * @author zhangdatong
 * @date 2021/05/17 9:49
 */
public class TestBoolean {


    @Test
    public void testComplexBooleanExpression() {
        boolean real = true;
        String line = "----------------++++";
        if (real || line.startsWith("---") && line.endsWith("---")) {
            System.out.println(true);
        } else {
            System.out.println(false);
        }
    }
}
