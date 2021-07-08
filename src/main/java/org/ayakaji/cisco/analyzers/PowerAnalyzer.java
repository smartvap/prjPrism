package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * show environment power命令结果解析
 * @author zhangdatong
 * @date 2021/06/09 9:02
 * @version 1.0.0
 */
@AnalyzerName("SHOW_ENVIRONMENT_POWER")
public class PowerAnalyzer extends ResultAnalyzer{

    public PowerAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    @Override
    public List<String> analysisResult() throws Exception {
        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String detail = buildDetailResult(lines);
        resultInfo.setDetailResult(detail);
        List<String> errorMsg = new ArrayList<String>();
        boolean beginRead = false;
        String target = null;
        List<String[]> powerSupply = new ArrayList<String[]>();
        List<String[]> moduleInfo = new ArrayList<String[]>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if(line.startsWith("---") && line.endsWith("---")) {
//                读表头, 并确定这次是读哪一组的信息
                String title = lines[i - 2];
                if (title == null || title.isEmpty() || title.trim().isEmpty()) {
                    continue;
                }
                beginRead = true;
                List<String> titles = parseProperties(title);
                String[] titleArr = titles.toArray(new String[0]);
                String firTitle = titleArr[0];
                if (firTitle.contains("Supply")) {
                    target = "power supply";
                    powerSupply.add(titleArr);
                } else {
                    target = "module";
                    moduleInfo.add(titleArr);
                }

                continue;
            }

            if (line == null || line.trim().isEmpty()) {
                beginRead = false;
                continue;
            }

            if (!beginRead) {
                continue;
            }

//            开始读取指标信息
            String[] props = parseProperties(line).toArray(new String[0]);
            if ("power supply".equalsIgnoreCase(target)) {
                powerSupply.add(props);
            } else {
                moduleInfo.add(props);
            }
        }

        /**
         * 解读采集到的信息
         */
//        规定读取指标的下标
        int[] readIdxs = new int[3];
        String errorMsgFormat = "show environment power 命令巡检结果异常，异常组 %s, 异常指标 %s, 异常状态 %s";
        /**
         * 先处理powerSupply段的信息
         */
        String[] titles = powerSupply.get(0);
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            if (title.contains("Supply")) {
                readIdxs[0] = i;
            }
            if (title.contains("Model")) {
                readIdxs[1] = i;
            }
            if (title.contains("Status")) {
                readIdxs[2] = i;
            }
        }
        for (int i = 1, len = powerSupply.size(); i < len; i++) {
            String[] props = powerSupply.get(i);
            String indicatorPrefix = props[readIdxs[0]];
            String indicatorBody = props[readIdxs[1]];
            String indicatorValue = props[readIdxs[2]];
            String indicatorName = String.join("_", indicatorPrefix, indicatorBody);
            resultInfo.addIndicator(indicatorName, indicatorValue);
            if (!"OK".equalsIgnoreCase(indicatorValue)) {
                errorMsg.add(String.format(errorMsgFormat, "Power Supply", indicatorName, indicatorValue));
            }
        }

        /**
         * 再处理moduleInfo的采集结果信息
         */
        titles = moduleInfo.get(0);
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            if ("Module".equalsIgnoreCase(title)) {
                readIdxs[0] = i;
            }
            if ("Model".equalsIgnoreCase(title)) {
                readIdxs[1] = i;
            }
            if ("Status".equalsIgnoreCase(title)) {
                readIdxs[2] = i;
            }
        }
        for (int i = 1, len = moduleInfo.size(); i < len; i++) {
            String[] props = moduleInfo.get(i);
            String indicatorPrefix = props[readIdxs[0]];
            String indicatorBody = props[readIdxs[1]];
            String indicatorValue = props[readIdxs[2]];
            String indicatorName = String.join(indicatorPrefix, indicatorBody);
            resultInfo.addIndicator(indicatorName, indicatorValue);
            if (!"Powered-Up".equalsIgnoreCase(indicatorValue)) {
                errorMsg.add(String.format(errorMsgFormat, "Module State", indicatorName, indicatorValue));
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
