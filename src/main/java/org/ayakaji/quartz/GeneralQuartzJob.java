package org.ayakaji.quartz;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GeneralQuartzJob implements Job {
	private static Logger LOGGER = Logger.getLogger(GeneralQuartzJob.class.getName());

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		DateTime dt = DateTime.now();
		LOGGER.info(context.getJobDetail().getKey().toString() + "-" + dt.toString("YYYYMMDDhh24mmss"));
	}

}