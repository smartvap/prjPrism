package org.ayakaji.testTasks;

import org.ayakaji.scheduling.DefaultCommonTask;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 测试周期执行类
 */
public class TestExecCronTask extends DefaultCommonTask {
    @Override
    public void preExecute(JobExecutionContext context) throws Exception {
        System.out.println("pre-execute method start");
        super.preExecute(context);
        JobKey key = context.getJobDetail().getKey();
        interrupt();
        Thread.sleep(60000);
        System.out.println("pre-execute method finished");
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        super.execute(context);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("task was executed cron at time" + format.format(new Date()));

    }

}
