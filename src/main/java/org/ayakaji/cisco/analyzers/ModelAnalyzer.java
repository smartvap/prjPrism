package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * show model 命令执行结果解析器
 * @author zhangdatong
 * @date 2021/06/08 18:14
 * @version 1.0.0
 */
@AnalyzerName("SHOW_MODULLE")
public class ModelAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(ModelAnalyzer.class);

    public ModelAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    @Override
    public List<String> analysisResult() throws Exception {
        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        result.setDetailResult(resultDetail);
//                解析结果信息
        List<List<String>> modList = new ArrayList<List<String>>();
        List<List<String>> xbarList = new ArrayList<List<String>>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("Mod")) {
                i++;
                List<String> colTitles = parseProperties(line);
                if (modList.isEmpty()) {
                    modList.add(colTitles);
                } else {
                    modList.get(0).addAll(colTitles);
                }
                int dataLineNum = 0;
                for (; i < lines.length; i++, dataLineNum++) {
                    String realLine = lines[i];
                    if (realLine.startsWith("--") && realLine.endsWith("--")) {
                        continue;
                    }
//                    如果是空行，读取结束
                    if (realLine.trim().isEmpty()) {
                        break;
                    }
                    List<String> properties = parseProperties(realLine);
                    if (modList.size() < dataLineNum + 1) {
                        modList.add(properties);
                    } else {
                        modList.get(dataLineNum).addAll(properties);
                    }
                }
            } else if (line.startsWith("Xbar")) {
                i++;
                List<String> colTitles = strictParseProperties(line);
                if (xbarList.isEmpty()) {
                    xbarList.add(colTitles);
                } else {
                    xbarList.get(0).addAll(colTitles);
                }
                int dataLineNum = 0;
                for (; i < lines.length; i++, dataLineNum++) {
                    String realLine = lines[i];
                    if (realLine.startsWith("--") && realLine.endsWith("--")) {
                        continue;
                    }
//                    如果是空行，读取结束
                    if (realLine.trim().isEmpty()) {
                        break;
                    }
                    List<String> properties = parseProperties(realLine);
                    if (xbarList.size() < dataLineNum + 1) {
                        xbarList.add(properties);
                    } else {
                        xbarList.get(dataLineNum).addAll(properties);
                    }
                }
            }
        }
        List<String> detectAlarm = new ArrayList<String>();
        List<String> modAlarms = analyzeModleResult(modList, result);

        List<String> xbarAlarms = analyzeXbarResult(xbarList, result);

        detectAlarm.addAll(modAlarms);
        detectAlarm.addAll(xbarAlarms);

        return detectAlarm;
    }

    /**
     * 解析modle结果信息
     * @param modelResult   modle 结果信息
     * @param result   探测结果信息
     * @return 告警信息
     */
    private List<String> analyzeModleResult(List<List<String>> modelResult, DetectResult result) {
        boolean hasError = false;
//                声明一个告警信息集合，收集所有的modle告警信息
        List<String> alarmInfo = new ArrayList<String>();
//                先确定我们所取的指标的列号及我们要作识别标识的字段的列号
        int statusIdx = -1;
        int onLineDialogStatusIdx = -1;
        int modIdx = -1;
        int moduleTypeIdx = -1;
        List<String> titles = modelResult.get(0);
        for (int i = 0, len = titles.size(); i < len; i++) {
            String title = titles.get(i);
            if (title.equalsIgnoreCase("Mod")) {
                modIdx = i;
            } else if (title.equalsIgnoreCase("Module-Type")) {
                moduleTypeIdx = i;
            } else if (title.equalsIgnoreCase("Status")) {
                statusIdx = i;
            } else if (title.equalsIgnoreCase("Online Diag Status")) {
                onLineDialogStatusIdx = i;
            }
        }
        XContentBuilder xBuilder = null;
        try {
            xBuilder = XContentFactory.jsonBuilder();
        } catch (IOException e) {
            logger.error("组织 CISCO Mod 探测结果信息异常");
        }
//              读取指标信息，查看是否合规
        for (int i = 1, len = modelResult.size(); i < len; i++) {
            List<String> data = modelResult.get(i);
//                    对Status字段进行判断，要求必须是  ok 或者active  或者 standy
            String statusInfo = data.get(statusIdx);
            String modelType = data.get(moduleTypeIdx);
            String mod = data.get(modIdx);
            String onLineDialogStatus = data.get(onLineDialogStatusIdx);
            if (!(statusInfo.equalsIgnoreCase("OK")
                    || statusInfo.contains("active") || statusInfo.contains("standby"))) {
                String alarmItem = String.format("Mod: %s, Modle-Type: %s, Status: %s",
                        mod, modelType, statusInfo);
                alarmInfo.add(alarmItem);
                hasError = true;
            }
//                    对Online Diag Status 字段进行判断，要求必须是 OK  pass  active  或者 standby
            if (!(onLineDialogStatus.equalsIgnoreCase("OK")
                    || onLineDialogStatus.equalsIgnoreCase("PASS")
                    || onLineDialogStatus.contains("active") || onLineDialogStatus.contains("standby"))) {
                String alarmItem = String.format("Mod: %s, Modle-Type: %s, Online Diag Status: %s",
                        mod, modelType, onLineDialogStatus);
                alarmInfo.add(alarmItem);
                hasError = true;
            }
        }
        result.addIndicator("modStatus", hasError ? "ERROR" : "OK");
        return alarmInfo;
    }

    /**
     * 解析Xbar结果信息
     * @param xbarResult xbar结果信息
     * @param result 探测结果信息
     * @return 告警信息
     */
    private List<String> analyzeXbarResult(List<List<String>> xbarResult, DetectResult result) {
        List<String> alarmInfo = new ArrayList<String>();
        List<String> titles = xbarResult.get(0);
        int xbarIdx = -1;
        int moduleTypeIdx = -1;
        int statusIdx = -1;
        int modelIdx = -1;
        boolean hasError = false;
//                解析表头
        for (int i = 0, len = titles.size(); i < len; i++) {
            String title = titles.get(i);
            if ("Xbar".equalsIgnoreCase(title)) {
                xbarIdx = i;
            } else if ("Module-Type".equalsIgnoreCase(title)) {
                moduleTypeIdx = i;
            } else if ("Model".equalsIgnoreCase(title)) {
                modelIdx = i;
            } else if ("Status".equalsIgnoreCase(title)) {
                statusIdx = i;
            }
        }
//                解析内容
        for (int i = 1, len = xbarResult.size(); i < len; i++) {
            List<String> data = xbarResult.get(i);
            String xbar = data.get(xbarIdx);
            String moduleType = data.get(moduleTypeIdx);
            String modle = data.get(modelIdx);
            String status = data.get(statusIdx);
            if (!"OK".equalsIgnoreCase(status)) {
                String alarmItem = String.format("Xbar: %s, Module-Type: %s, Model: %s, Status: %s");
                alarmInfo.add(alarmItem);
                hasError = true;
            }
        }
        result.addIndicator("XbarStatus", hasError ? "ERROR" : "OK");
        return alarmInfo;
    }
}
