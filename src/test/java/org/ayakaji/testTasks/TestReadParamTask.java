package org.ayakaji.testTasks;

import org.ayakaji.scheduling.DefaultCommonTask;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;

public class TestReadParamTask extends DefaultCommonTask {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String description = context.getTrigger().getDescription();
        System.out.println("get description: " + description);
        String param = context.getMergedJobDataMap().getString("param");
        System.out.println("get param: " + param);
    }
}
