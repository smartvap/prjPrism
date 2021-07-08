package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show system resources 执行结果分析器
 * @author zhangdatong
 * @date 2021/06/09 9:52
 * @version 1.0.0
 */
@AnalyzerName("SHOW_SYSTEM_RESOURCES")
public class SystemResourceAnalyzer extends ResultAnalyzer{

    public SystemResourceAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show system resources 执行结果分析，检查CPU和内存使用情况，过高需要告警(CPU>50,内存>30)
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

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
//                    CPU使用情况检查
            if (line.contains("CPU states")) {
                Pattern pattern = Pattern.compile("([\\d]*(.[\\d]*)?)% idle");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String indicator = matcher.group(1);
                    double indicatorVal = Double.valueOf(indicator);
                    double usedRate = 100 - indicatorVal;
                    String usedRatePercent = usedRate + "%";
                    resultInfo.addIndicator("CPU used", usedRatePercent);
                    if (usedRate > 50) {
                        errorMsg.add("CPU 使用率过高，" + usedRatePercent);
                    }
                }
            }
//                    内存使用情况检查
            if (line.contains("Memory usage")) {
                Pattern pattern = Pattern.compile("Memory usage:[ \\t\\r\\n\\f]*([\\d]*)K total, " +
                        "[ \\t\\r\\n\\f]*([\\d]*)K used");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    BigDecimal used = new BigDecimal(matcher.group(2));
                    BigDecimal total = new BigDecimal(matcher.group(1));
                    double useRate = used.multiply(new BigDecimal(100)).divide(total,
                            new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
                    String useRatePercent = useRate + "%";
                    resultInfo.addIndicator("memory used", useRatePercent);
                    if (useRate > 30) {
                        errorMsg.add("内存使用率过高：" + useRatePercent);
                    }
                }
            }
        }

        if (resultInfo.getIndicators().isEmpty()) {
            resultInfo.addIndicator("Exception", "未获得期望的返回信息");
            errorMsg.add("show system resources 执行结果信息异常，未获得期望的返回信息");
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
