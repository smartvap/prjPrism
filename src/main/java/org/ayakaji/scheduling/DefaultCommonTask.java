package org.ayakaji.scheduling;

import org.quartz.*;

import java.io.IOException;

/**
 * 默认的开发公共Task抽象类，包含执行前预处理方法和执行后的后处理方法，建议所有的任务都继承此类
 * @author zhangdatong
 * @version 1.0.0
 */
public abstract class DefaultCommonTask implements InterruptableJob {

    protected boolean _interrupted = false;
    protected String _interruptReason;

    /**
     * 预处理方法，在执行任务之前触发
     * @param context
     */
    public void preExecute(JobExecutionContext context) throws IOException, Exception {

    }

    /**
     * 执行方法，必须实现的方法
     * @param context
     * @throws JobExecutionException
     */
    @Override
    public  void execute(JobExecutionContext context) throws JobExecutionException {
        if (_interrupted) {
            throw new JobExecutionException("task interrupted because of " + _interruptReason);
        }
    }

    /**
     * 后处理方法，在执行任务之后触发，包含执行任务时出现的异常
     * @param context
     * @param exception
     */
    public void executed(JobExecutionContext context, JobExecutionException exception) {

    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        _interrupted = true;
    }

    public void interrupt(String message) {
        _interrupted = true;
        _interruptReason = message;
    }
}
