package org.ayakaji.testTasks;

import org.ayakaji.scheduling.ScheduleDeamon;

/**
 * @author zhangdatong
 * @date 2021/06/16 13:53
 */
public class TestTask {
    public static void main(String[] args) {
        ScheduleDeamon.getInstance().start();
    }
}
