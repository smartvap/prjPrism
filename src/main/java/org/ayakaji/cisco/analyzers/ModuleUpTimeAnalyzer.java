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
 * show module uptime命令执行结果解析器
 * @author zhangdatong
 * @date 2021/06/09 9:11
 * @version 1.0.0
 */
@AnalyzerName("SHOW_MODULE_UPTIME")
public class ModuleUpTimeAnalyzer extends ResultAnalyzer{


    public ModuleUpTimeAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * how module uptime命令执行结果解析与上报
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
        Pattern filterPattern = Pattern.compile("[ \\f\\r\\t\\n]*[\\-]{1,}([^\\-]*)[\\-]{1,}");
        Pattern indicatorPattern = Pattern.compile("Up Time:[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*day[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*hour[s]?,[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*minute[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*second[s]?");
        String indicatorFormat = "%s %s:%s:%s";
        String errorMsgFormat = "Show module uptime 命令巡检异常， Module: %s, UpTime: %s";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher filterMatcher = filterPattern.matcher(line);
            String indicatorName = null;
            if (filterMatcher.find()) {
                indicatorName = filterMatcher.group(1);
                String indicatorValLine = lines[i += 2];
                Matcher valMatcher = indicatorPattern.matcher(indicatorValLine);
                String indicatorValue = null;
                if (valMatcher.find()) {
                    indicatorValue = String.format(indicatorFormat, valMatcher.group(1), valMatcher.group(2),
                            valMatcher.group(3), valMatcher.group(4));
                    String upDayStr = valMatcher.group(1);
                    int updays = Integer.valueOf(upDayStr).intValue();
                    if (updays == 0) {
                        String errorItem = String.format(errorMsgFormat, indicatorName, indicatorValue);
                        errorMsg.add(errorItem);
                    }
                }
                resultInfo.addIndicator(indicatorName, indicatorValue);
            }
        }
        if (errorMsg.size() > 0) {
            resultInfo.setLevel(AlarmLevel.ALARMING);
        } else {
            resultInfo.setLevel(AlarmLevel.INFO);
        }
        sendDetectResult(resultInfo);
        return errorMsg;
    }
}
