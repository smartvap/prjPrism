package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * show environment fan命令执行结果解析器
 * @author zhangdatong
 * @date 2021/06/09 9:26
 * @version 1.0.0
 */
@AnalyzerName("SHOW_ENVIRONMENT_FAN")
public class EnvironmentFanAnalyzer extends ResultAnalyzer{


    public EnvironmentFanAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show environment fan命令执行结果解析与上报
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {
        List<String> errorMsg = new ArrayList<String>();
        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String detail = String.join("\n", lines);
        resultInfo.setDetailResult(detail);
        List<String[]> data = new ArrayList<String[]>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("Fan:")) {
                String titleLine = lines[i+=2];
                String[] titles = parseProperties(titleLine).toArray(new String[0]);
                data.add(titles);
                i++;
                continue;
            }

            if (line.contains("Fan Zone Speed:")) {
                break;
            }

            String[] dataItem = parseProperties(line).toArray(new String[0]);
            data.add(dataItem);
        }
        int[] readColIndexs = new int[2];
        String[] colTitles = data.get(0);
        for (int i = 0; i < colTitles.length; i++) {
            String title = colTitles[i];
            if ("Fan".equalsIgnoreCase(title)) {
                readColIndexs[0] = i;
            }
            if ("Status".equalsIgnoreCase(title)) {
                readColIndexs[1] = i;
            }
        }
        String errorFormat = "show environment fan命令巡检结果异常， %s : %s";
        for (int i = 1, len = data.size(); i < len; i++) {
            String[] dataItem = data.get(i);
            String indicatorName = dataItem[readColIndexs[0]];
            String indicatorValue = dataItem[readColIndexs[1]];
            resultInfo.addIndicator(indicatorName, indicatorValue);
            if (!"Ok".equalsIgnoreCase(indicatorValue)) {
                String errorItem = String.format(errorFormat, indicatorName, indicatorValue);
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
