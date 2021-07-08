package org.ayakaji.util;

import java.util.concurrent.*;

/**
 * 线程池，负责除定时任务之外的异步处理
 * @author zhangdatong
 * @version 1.0.0
 *  date 2021/5/6
 */
public class ThreadPool {

    private static ExecutorService executors;

    /**
     * 定义线程池，默认最大50并发，线程存活时间为1分钟
     */
    static {
        executors = new ThreadPoolExecutor(0, 50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 执行线程
     * @param exe   线程类
     */
    public static void execute(Runnable exe) {
        executors.execute(exe);
    }

    public static <T> Future<T> submit(Callable<T> task) {
       Future<T> executedResult = executors.submit(task);
       return executedResult;
    }
}
