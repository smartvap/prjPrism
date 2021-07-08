package org.ayakaji.cisco.exceptions;

import org.ayakaji.pojo.TaskDealStrategy;

import java.util.concurrent.TimeUnit;

/**
 * 同时有多个用户登录异常，对于使用telnet或bsh连接服务器进行探测的任务，不可以在多用户同时登录时进行探测，如果出现此类情况，需要推迟5分钟以后再执行
 * @author zhangdatong
 * @date 2021/06/16 9:53
 */
public class MultiLoginedUsersException extends Exception{

    public static final TaskDealStrategy DEAL_STRATEGY = TaskDealStrategy.DELAY;    //设置处置策略为推迟执行

    public static final int WAIT_TIME = 5;  //设置推迟时长为5分钟

    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    public MultiLoginedUsersException() {
        super("There is one or more users logined currently,wait 5 minutes to detect");
    }
}
