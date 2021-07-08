package org.ayakaji.testClass;

import org.ayakaji.cisco.analyzers.AnalyzerFactory;
import org.ayakaji.cisco.analyzers.ResultAnalyzer;
import org.ayakaji.cisco.exceptions.MultiLoginedUsersException;
import org.ayakaji.pojo.TaskDealStrategy;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * @author zhangdatong
 * @date 2021/06/08 10:30
 */
public class ClassFindDemos {

    @Test
    public void testFileImplements() throws ClassNotFoundException {
        Package pack = ResultAnalyzer.class.getPackage();
        String packageName = pack.getName();
    }

    @Test
    public void testFindAnalyzer () {
        Class<? extends ResultAnalyzer> analyzerType = AnalyzerFactory.getAnalyzerTypeByName("UNKNOWN");
        System.out.println(analyzerType.getName());
    }

    @Test
    public void testReflectField() throws NoSuchFieldException, IllegalAccessException {
        Throwable exception = new MultiLoginedUsersException();
        Field field = exception.getClass().getField("DEAL_STRATEGY");
        System.out.println(field.getGenericType());
        System.out.println(field.get(exception));
        TaskDealStrategy strategy = TaskDealStrategy.class.cast(field.get(exception));
        switch (strategy) {
            case DELAY:
                System.out.println("delaying");
                break;
            case CANCEL:
                System.out.println("cancelling");
                break;
            default:
                System.out.println("unkown");
        }
    }
}
