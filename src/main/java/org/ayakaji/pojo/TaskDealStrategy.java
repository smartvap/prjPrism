package org.ayakaji.pojo;

/**
 * 任务处置方案，即处置策略，用于任务异常，提供处置方案标记
 * @author zhangdatong
 * @date 2021/06/16 10:12
 */
public enum TaskDealStrategy {
    DELAY, CANCEL;
}
