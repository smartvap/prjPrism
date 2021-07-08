package org.ayakaji.cisco.analyzers.anocation;

import java.lang.annotation.*;

/**
 * 执行结果解析器
 * @author zhangdatong
 * date :2021/6/8
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AnalyzerName {

    String value() default "";
}
