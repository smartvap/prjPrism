package org.ayakaji.quartz;

import java.util.Date;
import java.util.logging.Logger;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class GeneralQuartzScheduler {
	private static Logger LOGGER = Logger.getLogger(GeneralQuartzScheduler.class.getName());

	public static void main(String[] args) throws SchedulerException, InterruptedException {
		Scheduler scheduler = new StdSchedulerFactory().getScheduler();
		scheduler.start();
		JobKey jobKey = new JobKey("Job1", "General-1");
		JobDetail jobDetail = JobBuilder.newJob(GeneralQuartzJob.class).withIdentity(jobKey).build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Job1", "General-1")
				.startAt(new Date(System.currentTimeMillis() + 1000))
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever()).build();
		scheduler.scheduleJob(jobDetail, trigger);
		Thread.sleep(5000);
		scheduler.deleteJob(jobKey);
		scheduler.shutdown();
	}

}
