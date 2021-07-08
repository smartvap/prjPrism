package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show spanning-tree detail | in ago 命令结果解析器
 * @author zhangdatong
 * @date 2021/06/17 9:45
 * @version 1.0.0
 */
@AnalyzerName("SHOW_SPANNING_TREE")
public class SpanningTreeAnalyzer extends ResultAnalyzer {

    public SpanningTreeAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show spanning-tree detail | in ago 命令结果解析,主要排查后面的时间输出，输出为小时：分钟：秒，排查12小时以内的生成树抖动
     * 结果示例：
     * Number of topology changes 20 last change occurred 10981:17:10 ago
     *   Number of topology changes 72 last change occurred 5230:54:12 ago
     *   Number of topology changes 90 last change occurred 5230:54:12 ago
     *   Number of topology changes 11 last change occurred 10981:16:47 ago
     *   Number of topology changes 11 last change occurred 10981:16:47 ago
     *   Number of topology changes 11 last change occurred 10981:16:47 ago
     *   Number of topology changes 11 last change occurred 10981:16:47 ago
     *   Number of topology changes 20 last change occurred 10981:17:10 ago
     *   Number of topology changes 20 last change occurred 10981:17:10 ago
     *   Number of topology changes 11 last change occurred 10981:16:47 ago
     * @return 12小时以内的生成树抖动异常信息
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {

        List<String> errorMsg = new ArrayList<>();

        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        resultInfo.setDetailResult(resultDetail);

        Pattern occurTimePattern = Pattern.compile("([\\d]*):[\\d]*:[\\d]*");

        List<String> occurTimeList = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = occurTimePattern.matcher(line);
            if (matcher.find()) {
                String hourStr = matcher.group(1);
                int hours = Integer.valueOf(hourStr).intValue();
                if (hours < 12) {
                    errorMsg.add(line);
                }
                occurTimeList.add(matcher.group());
            }
        }

        String[] occurTimeArr = occurTimeList.toArray(new String[occurTimeList.size()]);
        String indicator = String.join(",", occurTimeArr);

        resultInfo.addIndicator("changeTimeList", indicator);

        if (errorMsg.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
        } else {
            resultInfo.setLevel(AlarmLevel.ALARMING);
        }

        sendDetectResult(resultInfo);

        if (!errorMsg.isEmpty()) {
            errorMsg.add(0, "show spanning-tree detail | in ago 命令结果异常");
        }

        return errorMsg;
    }
}
