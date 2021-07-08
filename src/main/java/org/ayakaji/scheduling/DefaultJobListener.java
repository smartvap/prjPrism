package org.ayakaji.scheduling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Assert;
import org.ayakaji.pojo.TaskDealStrategy;
import org.quartz.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度监听器，主要作用是将任务的执行过程分为执行前、执行触发异常和执行后三个部分,方便处理
 *
 * @author zhangdatong
 * @version 1.0.0
 * date 2021-04-14
 */
public class DefaultJobListener implements JobListener {

    private static transient Logger logger = LogManager.getLogger(DefaultJobListener.class);

    @Override
    public String getName() {
        return "DefaultJobListener";
    }

    /**
     * 任务执行前操作，从上下文中找到执行实体，并触发其“preExecute”方法
     *
     * @param context 任务执行的上下文
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        Job job = context.getJobInstance();
        JobKey key = context.getJobDetail().getKey();
        String taskDetail = context.getTrigger().getDescription();
        try {
            job.getClass().getDeclaredMethod("preExecute", JobExecutionContext.class).invoke(job, context);
        } catch (IllegalAccessException e) {
            logger.error("can not invoke pre-execute Mehod of task " + key + " : " + taskDetail + "skip it", e);
        } catch (NoSuchMethodException e) {
            logger.warn("task " + key + " : " + taskDetail + " can not find method before execute, try to execute it " +
                    "without pre-mehod");
        } catch (InvocationTargetException e) {
            Throwable exception = e.getTargetException();
            if (exception instanceof InterruptedException) {
                logger.error("interrupt command occured while pre-excuting task {}.{}, try to interrupt it",
                        key.getGroup(), key.getName());
                try {
                    if (!ScheduleDeamon.getInstance().getScheduler().interrupt(context.getFireInstanceId())) {
                        if (job instanceof DefaultCommonTask) {
                            ((DefaultCommonTask)job).interrupt(exception.getMessage());
                        } else if (job instanceof InterruptableJob) {
                            ((InterruptableJob) job).interrupt();
                        }
                    }
                } catch (UnableToInterruptJobException unableToInterruptJobException) {
                    logger.error("interrupt  task {}.{}, failed", key.getGroup(), key.getName(),
                            unableToInterruptJobException);
                }
                return;
            }

            //如果有处置策略的话，视情况予以处理
            try {
                Field dealStrategyFeild = exception.getClass().getDeclaredField("DEAL_STRATEGY");
                TaskDealStrategy strategy = TaskDealStrategy.class.cast(dealStrategyFeild.get(exception));
                switch (strategy) {
                    case DELAY:
                        if (!ScheduleDeamon.getInstance().getScheduler().interrupt(context.getFireInstanceId())) {
                            if (job instanceof DefaultCommonTask) {
                                ((DefaultCommonTask)job).interrupt(exception.getMessage());
                            } else if (job instanceof InterruptableJob) {
                                ((InterruptableJob) job).interrupt();
                            }
                        }
                        Field waitTimeField = exception.getClass().getDeclaredField("WAIT_TIME");
                        int waitTime = waitTimeField.getInt(exception);
                        Field waitTimeUnitField = exception.getClass().getDeclaredField("TIME_UNIT");
                        TimeUnit waitUnit = TimeUnit.class.cast(waitTimeUnitField.get(exception));
                        JobDataMap dataMap = context.getTrigger().getJobDataMap();
                        int retrys = 1;
                        try {
                            retrys = dataMap.getIntValue("retryTimes");
                            if (retrys < 4) {
                                dataMap.put("retryTimes", ++retrys);
                            } else {
                                logger.error("task {}-{} failed after retried 3 times, skip it", key.getGroup(),
                                        key.getName());
                                return;
                            }
                        } catch (Exception ex) {
                            dataMap.put("retryTimes", retrys);
                        }
                        TriggerKey triggerKey = TriggerKey.triggerKey(context.getFireInstanceId(), "dalaying" + retrys);
                        JobKey jobKey = JobKey.jobKey(context.getFireInstanceId(), "dalaying" + retrys);
                        Class<? extends Job> jobClass = job.getClass();
                        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobKey).storeDurably().build();

                        long delayTime = System.currentTimeMillis() + waitUnit.toMillis(waitTime);
                        Date fireTime = new Date(delayTime);

                        Trigger delayTrigger = TriggerBuilder.newTrigger().forJob(jobDetail).startAt(fireTime)
                                .usingJobData(dataMap).withIdentity(triggerKey).withPriority(10)
                                .withSchedule(SimpleScheduleBuilder.simpleSchedule()).withDescription(taskDetail)
                                .build();

                        ScheduleDeamon.getInstance().getScheduler().scheduleJob(jobDetail, delayTrigger);
                        return;
                    case CANCEL:
                        ScheduleDeamon.getInstance().getScheduler().deleteJob(context.getJobDetail().getKey());
                        return;
                    default:
                        logger.debug("nothing to deal with the exception Job");
                }

            } catch (NoSuchFieldException | IllegalAccessException |
                    UnableToInterruptJobException noSuchFieldException) {
            } catch (SchedulerException schedulerException) {
                logger.error("build schedule task failed", schedulerException);
            }
            logger.error("can not invoke pre-execute Mehod of task " + key + " : " + taskDetail + "skip it", e);
        }
    }

    /**
     * 这个方法正常情况下不执行,但是如果当TriggerListener中的vetoJobExecution方法返回true时,那么执行这个方法
     * 记录出现异常的任务的key和描述信息，并尝试从任务调度器中删除此任务
     *
     * @param context 任务执行的上下文
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        String trigger = key.getName();
        TriggerKey triggerKey = context.getTrigger().getKey();
        String taskDetail = context.getTrigger().getDescription();
        logger.error("can not execute task" + trigger + " : " + taskDetail + " try to abort it");
        try {
            context.getScheduler().unscheduleJob(triggerKey);
        } catch (SchedulerException e) {
            logger.error("Exception occured while deleting Task" + key, e);
        }
    }

    /**
     * 任务执行后操作，从上下文中找到执行实体，并触发其“executed”方法,如果执行任务的过程中有异常发生，记录异常信息但不作处理
     *
     * @param context 任务执行的上下文
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        Job job = context.getJobInstance();
        JobKey key = context.getJobDetail().getKey();
        String trigger = key.getName();
        String taskDetail = context.getTrigger().getDescription();
//        如果任务被打断，则不再继续执行
        if (jobException != null) {
            logger.error("Exception occured while executing task[{} : {}]", trigger, taskDetail, jobException);
        }
        try {
            job.getClass().getDeclaredMethod("executed", JobExecutionContext.class, JobExecutionException.class)
                    .invoke(job, context, jobException);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("can not invoke pre-execute Mehod of task " + key + " : " + taskDetail + "skip it", e);
        } catch (NoSuchMethodException e) {
            logger.warn("task " + key + " : " + taskDetail + " can not find method after execute, try to execute it " +
                    "without after-mehod");
        }
    }
}
