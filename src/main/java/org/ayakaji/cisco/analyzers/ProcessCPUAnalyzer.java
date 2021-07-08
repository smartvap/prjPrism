package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show processes cpu 执行结果分析器
 * @author zhangdatong
 * @date 2021/06/09 9:40
 * @version 1.0.0
 */
@AnalyzerName("SHOW_PROCESSES_CPU")
public class ProcessCPUAnalyzer extends ResultAnalyzer{

    public ProcessCPUAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show processes cpu 执行结果分析器与上报
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {

        List<String> errorMsg = new ArrayList<String>();
        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String detail = buildDetailResult(lines);
        resultInfo.setDetailResult(detail);

        Pattern indicatorPattern = Pattern.compile("CPU utilization for five seconds: ([\\d]*)%/([\\d]*)%; " +
                "one minute: ([\\d]*)%; five minutes: ([\\d]*)%");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = indicatorPattern.matcher(line);
            if (matcher.find()) {
                String fiveSecondsS = matcher.group(1);
                String fiveSecondsL = matcher.group(2);
                String oneMinuteS = matcher.group(3);
                String fiveMinuteS = matcher.group(4);
                resultInfo.addIndicator("5秒平均", fiveMinuteS.concat("%").concat("/")
                        .concat(fiveSecondsL).concat("%"));
                resultInfo.addIndicator("1分钟平均", oneMinuteS.concat("%"));
                resultInfo.addIndicator("5分钟平均", fiveMinuteS.concat("%"));
                int fiveSecondsCost1 = Integer.valueOf(fiveSecondsS);
                int fiveSecondsCost2 = Integer.valueOf(fiveSecondsL);
                int oneMinuteCost = Integer.valueOf(oneMinuteS);
                int fiveMinuteCost = Integer.valueOf(fiveMinuteS);
                if (fiveSecondsCost1 > 50 || fiveSecondsCost2 > 50 || oneMinuteCost > 50 || fiveMinuteCost > 50) {
                    String errorFormat = "5秒平均: %d%%/%d%%, 1分钟平均: %d%%, 5分钟平均: %d%%";
                    String errorItem = String.format(errorFormat, fiveSecondsCost1, fiveSecondsCost2,
                            oneMinuteCost, fiveMinuteCost);
                    errorMsg.add(errorItem);
                }
                break;
            }
        }

        if (resultInfo.getIndicators().isEmpty()) {
            errorMsg.add("未解析到期望的结果信息");
            resultInfo.addIndicator("Exception", "未解析到期望的结果信息");
        }

        if (errorMsg.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
        } else {
            resultInfo.setLevel(AlarmLevel.ALARMING);
        }

        sendDetectResult(resultInfo);

        return errorMsg;
    }
}
