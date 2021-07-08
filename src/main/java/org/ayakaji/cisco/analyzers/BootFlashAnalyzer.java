package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.CiscoMeticDetectTask;
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
 * dir bootflash://命令结果解析器
 * @author zhangdatong
 * @date 2021/06/09 13:30
 */
@AnalyzerName("DIR_BOOTFLASH")
public class BootFlashAnalyzer extends ResultAnalyzer{


    public BootFlashAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * dir bootflash:// 命令执行结果解析，带参，使用率超过70%，告警
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

        Pattern usedPattern = Pattern.compile("([\\d]*) bytes used");
        Pattern totalPattern = Pattern.compile("([\\d]*) bytes total");
        BigDecimal usedBytes = null;
        BigDecimal totalBytes = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = usedPattern.matcher(line);
            if (matcher.find()) {
                String usedStr = matcher.group(1);
                usedBytes = new BigDecimal(usedStr);
                continue;
            }
            matcher = totalPattern.matcher(line);
            if (matcher.find()) {
                String totalStr = matcher.group(1);
                totalBytes = new BigDecimal(totalStr);
                continue;
            }
        }

        if (usedBytes == null || totalBytes == null) {
            resultInfo.addIndicator("Exception", "未获得期望的信息");
            errorMsg.add(command + "" + argument + " 执行结果异常，未获得期望的信息");
        } else {
            double usedRate = usedBytes.multiply(new BigDecimal(100)).divide(totalBytes,
                    new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
            resultInfo.addIndicator("useRate", usedRate + "%");
            if (usedRate > 70) {
                errorMsg.add(command + "" + argument + "指标异常：" + usedRate + "%");
            }
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
