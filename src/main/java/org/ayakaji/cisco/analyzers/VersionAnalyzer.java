package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show version 命令执行结果分析器
 * @author zhangdatong
 * @date 2021/06/08 17:57
 * @version 1.0.0
 */
@AnalyzerName("SHOW_VERSION")
public class VersionAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(VersionAnalyzer.class);

    public VersionAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show version 命令执行结果分析并上报分析结果
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {
        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        String updateTimeRegex = "Kernel uptime is (\\d*) day\\(s\\), (\\d*) hour\\(s\\), (\\d*) minute\\(s\\), (\\d*) second\\(s\\)";
        Pattern pattern = Pattern.compile(updateTimeRegex);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        result.setDetailResult(resultDetail);

        String updateTimeIndicator = null;    //开机时间指标
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                updateTimeIndicator = String.format("%s %s:%s:%s", matcher.group(1), matcher.group(2), matcher.group(3),
                        matcher.group(4));
                break;
            }
        }

        if (result == null) {
            result.addIndicator("Exception", "在命令返回结果中未找到期望的内容");
            Exception unExceptedResultException = buildUnExceptedResultException(command, argument, lines);
            logger.error("CISCO 探测结果分析异常，执行命令 {} ，参数 {} 未找到期望的指标信息", command, argument,
                    unExceptedResultException);
        } else {
            result.addIndicator("Uptime", updateTimeIndicator);
        }

        sendDetectResult(result);
        return null;
    }
}
