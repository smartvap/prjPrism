package org.ayakaji.network.prism;

import org.ayakaji.scheduling.ScheduleDeamon;
import org.quartz.JobKey;

public class ApplicationTest {
    public static void main(String[] args) throws InterruptedException {
        ScheduleDeamon.getInstance().start();
        Thread.sleep(5000L);
        ScheduleDeamon.getInstance().interruptTask(JobKey.jobKey("InterruptTestTask", "customTask"));
    }
}
