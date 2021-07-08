package org.ayakaji.pojo;

/**
 * 告警级别枚举类，按紧急程序划分等级
 * @author zhangdatong
 * @version 1.0.0
 * date: 2021.04.28
 */
public enum AlarmLevel {

    SERVER,     //需要服务器接收后处理的信息（一般用作历史记录的比对）
    UNKNOWN,     //不知道的信息，用于没有明确解析出来的监测日志信息
    INFO,        //正常信息，用于正常监测结果
    WARNING,     //预警信息，已出现问题，但问题并不紧急
    ALARMING,    //告警信息，已出现问题，且问题比较紧急
    URGENT      //紧急信息，已出现问题且已导致严重后果（如服务器宕机、服务不可用、网络不通等）
}
