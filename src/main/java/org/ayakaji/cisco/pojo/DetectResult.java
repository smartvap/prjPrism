package org.ayakaji.cisco.pojo;

import org.ayakaji.pojo.AlarmLevel;
import org.ayakaji.util.IniConfigFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用探测结果实体类
 * @author zhangdatong
 * @date 2021/06/08 16:09
 * @version 1.0.0
 */
public class DetectResult {
    private String host;    //主机名
    private String ipAddr;
    private String command;     //执行命令
    private String argument;    //命令参数
    private String realCommand; //真实执行的命令信息
    private AlarmLevel level;        //告警等级
    private Map<String, String> indicators;     //指标信息，如果是异常则会记录一个Exception,value是Exception的message信息
    private String detailResult;    //结果明细如果是异常，则记录的是异常的堆栈信息
    private Date detectTime;    //所属探测活动的触发时间

    public DetectResult(String hostName, String command, String argument, Date detectTime) {
        this.host = hostName;
        this.command = command;
        this.ipAddr = IniConfigFactory.getHostConfigSection(hostName).get("mgr_ip");
        this.argument = argument;
        this.realCommand = argument == null ? command : String.join(" ", command, argument);
        this.detectTime = detectTime;
        indicators = new HashMap<String, String>();
    }

    public void addIndicator(String indicatorName, String indicatorVal) {
        indicators.put(indicatorName, indicatorVal);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public String getRealCommand() {
        return realCommand;
    }

    public void setRealCommand(String realCommand) {
        this.realCommand = realCommand;
    }

    public AlarmLevel getLevel() {
        return level;
    }

    public void setLevel(AlarmLevel level) {
        this.level = level;
    }

    public Map<String, String> getIndicators() {
        return indicators;
    }

    public void setIndicators(Map<String, String> indicators) {
        this.indicators = indicators;
    }

    public String getDetailResult() {
        return detailResult;
    }

    public void setDetailResult(String detailResult) {
        this.detailResult = detailResult;
    }

    public Date getDetectTime() {
        return detectTime;
    }

    public void setDetectTime(Date detectTime) {
        this.detectTime = detectTime;
    }
}
