package org.ayakaji.testTasks;

import org.ayakaji.scheduling.DefaultCommonTask;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 测试定时执行类
 */
public class TestExecTimedTask extends DefaultCommonTask {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("task was executed timed at time" + format.format(new Date()));
    }
}
