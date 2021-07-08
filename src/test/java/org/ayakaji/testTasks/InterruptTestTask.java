package org.ayakaji.testTasks;

import org.ayakaji.cisco.exceptions.MultiLoginedUsersException;
import org.ayakaji.scheduling.DefaultCommonTask;
import org.quartz.*;

import java.io.IOException;

/**
 * 测试在预处理时直接打断执行任务
 */
public class InterruptTestTask extends DefaultCommonTask {
    @Override
    public void preExecute(JobExecutionContext context) throws IOException, Exception {
        System.out.println("before execute");
        throw new MultiLoginedUsersException();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        super.execute(context);
        System.out.println("execute");
    }

    @Override
    public void executed(JobExecutionContext context, JobExecutionException exception) {
        if (exception != null) {
            System.out.println("get interrupt Exception");
            return;
        }
        System.out.println("still run executed method");
    }
}
