package org.ayakaji.scheduling;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * 任务调度监听器，主要作用是将任务的执行过程分为执行前、执行触发异常和执行后三个部分,方便处理
 * @author zhangdatong
 * @version 1.0.0
 * date 2021-04-14
 */
public class DefaultJobListener implements JobListener {

    private static transient Logger logger = LoggerFactory.getLogger(DefaultJobListener.class);

    @Override
    public String getName() {
        return "DefaultJobListener";
    }

    /**
     * 任务执行前操作，从上下文中找到执行实体，并触发其“preExecute”方法
     * @param context 任务执行的上下文
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        Job job = context.getJobInstance();
        JobKey key = context.getJobDetail().getKey();
        String taskDetail = context.getTrigger().getDescription();
        try {
            job.getClass().getDeclaredMethod("preExecute", JobExecutionContext.class).invoke(job, context);
        } catch (IllegalAccessException|InvocationTargetException e) {
            logger.error("can not invoke pre-execute Mehod of task " + key + " : " + taskDetail + "skip it", e);
        } catch (NoSuchMethodException e) {
            logger.warn("task " + key + " : " + taskDetail + " can not find method before execute, try to execute it " +
                    "without pre-mehod");
        }
    }

    /**
     * 这个方法正常情况下不执行,但是如果当TriggerListener中的vetoJobExecution方法返回true时,那么执行这个方法
     * 记录出现异常的任务的key和描述信息，并尝试从任务调度器中删除此任务
     * @param context 任务执行的上下文
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        String trigger = key.getName();
        String taskDetail = context.getTrigger().getDescription();
        logger.error("can not execute task" + trigger + " : " + taskDetail + " try to abort it");
        try {
            context.getScheduler().deleteJob(key);
        } catch (SchedulerException e) {
            logger.error("Exception occured while deleting Task" + key, e);
        }
    }

    /**
     * 任务执行后操作，从上下文中找到执行实体，并触发其“executed”方法,如果执行任务的过程中有异常发生，记录异常信息但不作处理
     * @param context 任务执行的上下文
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        Job job = context.getJobInstance();
        JobKey key = context.getJobDetail().getKey();
        String trigger = key.getName();
        String taskDetail = context.getTrigger().getDescription();
        if (jobException != null) {
            logger.error("Exception occured while executing task[{} : {}]", trigger, taskDetail, jobException);
        }
        try {
            job.getClass().getDeclaredMethod("executed", JobExecutionContext.class, JobExecutionException.class)
                    .invoke(job, context, jobException);
        } catch (IllegalAccessException|InvocationTargetException e) {
            logger.error("can not invoke pre-execute Mehod of task " + key + " : " + taskDetail + "skip it", e);
        } catch (NoSuchMethodException e) {
            logger.warn("task " + key + " : " + taskDetail + " can not find method after execute, try to execute it " +
                    "without pre-mehod");
        }
    }
}
