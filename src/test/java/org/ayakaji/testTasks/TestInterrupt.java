package org.ayakaji.testTasks;

import org.ayakaji.scheduling.ScheduleDeamon;
import org.junit.Test;
import org.quartz.*;

import java.util.List;

public class TestInterrupt {

    @Test
    public void testInterrupt() throws SchedulerException {
        TriggerKey key = TriggerKey.triggerKey("InterruptTestTask", "customTask");
        Trigger.TriggerState state = ScheduleDeamon.getInstance().getScheduler().getTriggerState(key);
        System.out.println(state);
        ScheduleDeamon.getInstance().getScheduler().pauseTrigger(key);
        ScheduleDeamon.getInstance().getScheduler().unscheduleJob(key);
        JobKey jobKey = new JobKey("TestExecCronTask", "customTask");
        ScheduleDeamon.getInstance().getScheduler().deleteJob(jobKey);
        state = ScheduleDeamon.getInstance().getScheduler().getTriggerState(key);
        boolean jobExists = ScheduleDeamon.getInstance().getScheduler().checkExists(
                JobKey.jobKey("TestExecCronTask", "customTask"));
        System.out.println(state);
        System.out.println(jobExists);
    }

    @Test
    public void checkExistTask() throws SchedulerException {
        Scheduler shed = ScheduleDeamon.getInstance().getScheduler();
        boolean taskExist = shed.checkExists(JobKey.jobKey("InterruptTestTask", "customTask"));
        System.out.println(taskExist);
    }

    @Test
    public void deleteTask() throws SchedulerException {
        Scheduler shed = ScheduleDeamon.getInstance().getScheduler();
        JobKey jobKey = JobKey.jobKey("InterruptTestTask", "customTask");
        boolean result = shed.deleteJob(jobKey);
        System.out.println("delete task result " + result);
    }

    @Test
    public void unscheduleTask() throws SchedulerException {
        Scheduler shed = ScheduleDeamon.getInstance().getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey("InterruptTestTask", "customTask");
        boolean result = shed.unscheduleJob(triggerKey);
        System.out.println("unschedule task result " + result);
    }

    @Test
    public void interruptTask() throws UnableToInterruptJobException {
        Scheduler shed = ScheduleDeamon.getInstance().getScheduler();
        JobKey jobKey = JobKey.jobKey("InterruptTestTask", "customTask");
//        ScheduleDeamon.getInstance().interruptTask(jobKey);
//        System.out.println("interrupt task result " + result);
    }

    @Test
    public void testInterruptTaskSelf() {
        ScheduleDeamon.getInstance();
    }
}
