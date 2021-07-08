package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.ayakaji.pojo.AlarmLevel;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show redundancy status 结果信息解析
 * @author zhangdatong
 * @date 2021/06/09 8:53
 * @version 1.0.0
 */
@AnalyzerName("SHOW_REDUNDANCY_STATUS")
public class RedundancyStateAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(RedundancyStateAnalyzer.class);

    public RedundancyStateAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * 结果信息解析，对比历史记录，当This supervisor或Other supervisor的状态发生变更时，要告警，并更新ES中保存的记录
     * 同时要保存探测记录信息
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {
        List<String> errorMsg = new ArrayList<String>();

        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);

        String thisSupervisorState = null;
        String otherSupervisorState = null;
        String thisSupervisorInternalState = null;
        String otherSupervisorInternalState = null;
        boolean restartRecently = false;
        String target = null;
        Pattern pattern = Pattern.compile("[\\s\\S]*0[^\\d]*day[\\s\\S]*");

        String[] lines = (String[])detectResult;

        resultInfo.setDetailResult(buildDetailResult(lines));

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("This supervisor")) {
                target = "This supervisor";
                continue;
            }
            if (line.contains("Other supervisor")) {
                target = "Other supervisor";
                i += 3;
                continue;
            }
            if (line == null || line.isEmpty() || line.trim().isEmpty() || line.startsWith("---")
                    && line.endsWith("---")) {
                continue;
            }

            if (line.contains("Supervisor state")) {
                String[] entry = line.split(":");
                String val = entry[1].trim();
                if ("This supervisor".equals(target)) {
                    thisSupervisorState = val;
                } else {
                    otherSupervisorState = val;
                }
                continue;
            }

            if (line.contains("Internal state:")) {
                String[] entry = line.split(":");
                String val = entry[1].trim();
                if ("This supervisor".equals(target)) {
                    thisSupervisorInternalState = val;
                } else {
                    otherSupervisorInternalState = val;
                }
                continue;
            }


            if (line.contains("System uptime:") || line.contains("Kernel uptime:") ||
                    line.contains("Active supervisor uptime:")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    errorMsg.add(line);
                }
                String[] uptimeIndicator = line.split(":");
                resultInfo.addIndicator(uptimeIndicator[0].trim(), uptimeIndicator[1].trim());
            }

        }

        String statusId = hostName.toLowerCase().replaceAll(" ", "_");
//                和历史数据作比对，如果比对信息不一致，则需要告警
        try {
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            GetRequest finder = new GetRequest("cisco_redundancy_status", statusId);
            boolean hasHistory = client.exists(finder, RequestOptions.DEFAULT);

            if (hasHistory) {
                GetResponse historyRecord = client.get(finder, RequestOptions.DEFAULT);
                ElasticSearchPoolUtil.returnClient(client);
                Map<String, String> thisSupervisor = (Map<String, String>)historyRecord.getSource().get("thisSupervisor");
                Map<String, String> otherSupervisor = (Map<String, String>)historyRecord.getSource().get("otherSupervisor");
                String thisState = thisSupervisor.get("state");
                String thisInternalState = thisSupervisor.get("internalState");
                String otherState = otherSupervisor.get("state");
                String otherInternalState = otherSupervisor.get("internalState");
                String stateErrorFormat = "REDUNDANCY_STATUS 对比异常，%s 指标：%s, 历史值为：%s, 当前值为：%s";

                boolean matched = true;
                if (!thisState.equals(thisSupervisorState)) {
                    errorMsg.add(String.format(stateErrorFormat, "thisSupervisor", "Supervisor state", thisState,
                            thisSupervisorState));
                    matched = false;
                }
                resultInfo.addIndicator("thisSupervisor[Supervisor state]", String.join(":",
                        thisSupervisorState, thisState));

                if (!thisInternalState.equals(thisSupervisorInternalState)) {
                    errorMsg.add(String.format(stateErrorFormat, "thisSupervisor", "Internal state",
                            thisInternalState, thisSupervisorInternalState));
                    matched = false;
                }
                resultInfo.addIndicator("thisSupervisor[Internal state]", String.join(":",
                        thisSupervisorInternalState, thisInternalState));

                if (!otherState.equals(otherSupervisorState)) {
                    errorMsg.add(String.format(stateErrorFormat, "otherSupervisor", "Supervisor state", otherState,
                            otherSupervisorState));
                    matched = false;
                }
                resultInfo.addIndicator("otherSupervisor[Supervisor state]", String.join(":",
                        otherSupervisorState, otherState));

                if (!otherInternalState.equals(otherSupervisorInternalState)) {
                    errorMsg.add(String.format(stateErrorFormat, "otherSupervisor", "Internal state",
                            otherInternalState, otherSupervisorInternalState));
                    matched = false;
                }
                resultInfo.addIndicator("otherSupervisor[Internal state]", String.join(":",
                        otherSupervisorInternalState, otherInternalState));


                if (!matched) {
                    resultInfo.setLevel(AlarmLevel.ALARMING);
                    upsertSupervisorStateInfo(hostName, thisSupervisorState, thisSupervisorInternalState,
                            otherSupervisorState, otherSupervisorInternalState);
                }
            } else {
                ElasticSearchPoolUtil.returnClient(client);
                upsertSupervisorStateInfo(hostName, thisSupervisorState, thisSupervisorInternalState,
                        otherSupervisorState, otherSupervisorInternalState);
            }

            sendDetectResult(resultInfo);

        } catch (Exception e) {
            logger.warn("核查show redundancy status 命令执行结果时出现故障，无法得到历史信息", e);
            logger.error("核查show redundancy status 命令执行结果时,无法获得ElasticSearch连接", e);
        }

        return errorMsg;
    }

    /**
     * 保存SupervisorState信息
     * @param hostname  主机名称
     * @param thisSupervisorState   当前监视器状态
     * @param thisSupervisorInternalState   当前监视器内核状态
     * @param otherSupervisorState  其他监视器状态
     * @param otherSupervisorInternalState  其他监视器内核状态
     */
    private void upsertSupervisorStateInfo(String hostname, String thisSupervisorState,
                                           String thisSupervisorInternalState, String otherSupervisorState,
                                           String otherSupervisorInternalState) {

        IndexRequest request = new IndexRequest("cisco_redundancy_status");
        RestHighLevelClient client = null;
        try {
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder();
            contentBuilder.startObject();
            contentBuilder.startObject("thisSupervisor");
            {
                contentBuilder.field("state", thisSupervisorState);
                contentBuilder.field("internalState", thisSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.startObject("otherSupervisor");
            {
                contentBuilder.field("state", otherSupervisorState);
                contentBuilder.field("internalState", otherSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.endObject();
            client = ElasticSearchPoolUtil.getClient();
            String infoKey = hostname.toLowerCase().replaceAll(" ", "_");
            request.id(infoKey);
            request.source(contentBuilder);
            request.timeout(TimeValue.timeValueSeconds(3));
            client.index(request, RequestOptions.DEFAULT);

        } catch (Exception e) {
            logger.error("Can not save new Supervisor state info of host {}", hostname, e);
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }
    }
}
