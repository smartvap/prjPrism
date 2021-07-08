package org.ayakaji.testTasks;

import org.ayakaji.scheduling.DefaultCommonTask;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 测试立即执行类
 */
public class TestExecImmediateTask extends DefaultCommonTask {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("task executed immediately");
    }
}
