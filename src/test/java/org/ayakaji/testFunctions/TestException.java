package org.ayakaji.testFunctions;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class TestException {

    @Test
    public void testGetExceptionStackString() {
        try {
            throw new IllegalArgumentException("test");
        } catch (IllegalArgumentException e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            System.out.print(writer.toString());
        }
    }
}
