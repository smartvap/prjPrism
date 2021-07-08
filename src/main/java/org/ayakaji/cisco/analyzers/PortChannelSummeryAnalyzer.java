package org.ayakaji.cisco.analyzers;

import org.apache.http.util.Asserts;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.ayakaji.pojo.AlarmLevel;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * show port-channel summary 命令结果解析器
 * @author zhangdatong
 * @date 2021/06/23 10:57
 */
@AnalyzerName("SHOW_PORT_CHANNEL_SUMMARY")
public class PortChannelSummeryAnalyzer extends ResultAnalyzer {
    public PortChannelSummeryAnalyzer(String hostName, String command, String argument, Object detectResult,
                                      Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * Show port-channel summary命令结果解析方法，与历史数据进行比对，若Member Ports字段中interface 的status发生改变，则直接按Port-
     * Channel:interface change message 的方式进行告警, 探测完成以后，更新本主机的接口状态信息同时保存探测日志信息
     * 探测结果示例：
     * Flags:  D - Down        P - Up in port-channel (members)
     *         I - Individual  H - Hot-standby (LACP only)
     *         s - Suspended   r - Module-removed
     *         S - Switched    R - Routed
     *         U - Up (port-channel)
     *         M - Not in use. Min-links not met
     *         --------------------------------------------------------------------------------
     * Group Port-       Type                Protocol  Member Ports
     *       Channel
     * --------------------------------------------------------------------------------
     * 1     Po1(SU)     Eth      NONE      Eth1/39(P)   Eth1/40(P)   Eth7/39(P)
     *                                       Eth7/40(P)
     * 3     Po3(SU)     Eth      NONE      Eth1/24(P)   Eth7/24(P)
     * 10    Po10(SU)    Eth      LACP       Eth1/27(P)   Eth7/27(P)
     * 91    Po91(SU)    Eth      NONE      Eth1/25(D)   Eth7/25(P)
     * 98    Po98(SU)    Eth      NONE      Eth1/1(P)
     * 101   Po101(SU)   Eth      NONE      Eth1/33(P)   Eth1/34(P)   Eth7/33(P)
     *                                       Eth7/34(P)
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {

        List<String> errorMsg = new ArrayList<>();

        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        resultInfo.setDetailResult(resultDetail);

        boolean beginRead = false;

        int colNum = -1;

        int focusIdx = -1;

        int identifyIdx = -1;

        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*\\/\\d*)\\(([a-zA-Z]*)\\)");

        Map<String, String> oldIndicators = getHistoryIndicators(resultInfo.getIpAddr());

        boolean hasHistory = !oldIndicators.isEmpty();

        Map<String, String> curInterfacesStatus = new HashMap<>();

        String errorMsgFormatter = "interface status change in Port-Channel %s : %s";
        String errorIndicatorFormatter = "interface %s status changed from %s to %s";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("---") && line.endsWith("---")) {
                line = lines[++i];
                List<String> titles = parseProperties(line);
                colNum = titles.size();

                for (int j = 0; j < colNum; j++) {
                    String title = titles.get(j);
                    if (title.equalsIgnoreCase("Port-Channel")) {
                        identifyIdx = j;
                        continue;
                    } else if (title.equalsIgnoreCase("Member Ports")) {
                        focusIdx = j;
                        continue;
                    }
                    if (identifyIdx >= 0 && focusIdx >= 0) {
                        break;
                    }
                }

                for(; !(line.startsWith("---") && line.endsWith("---")) && i < lines.length; line = lines[++i]);
                beginRead = true;
                continue;
            }

            if (!beginRead) {
                continue;
            }

            if (colNum < 0 || focusIdx < 0 || identifyIdx < 0) {
                errorMsg.add("can not find useful info from command execute result");
                resultInfo.addIndicator("Exception",
                        "can not find useful info from command execute result");
                resultInfo.setLevel(AlarmLevel.ALARMING);
                break;
            }


            List<String> properties = parseProperties(line, colNum);

            String focusVal = properties.get(focusIdx);
            String identifyTitle = properties.get(identifyIdx);

            resultInfo.addIndicator(identifyTitle, focusVal);

            Matcher matcher = pattern.matcher(focusVal);

            StringBuilder errorTextbuilder = new StringBuilder();

            while (matcher.find()) {
                String interfaceName = matcher.group(1);
                String status = matcher.group(2);
                if (hasHistory) {
                    String oldStatus = oldIndicators.get(interfaceName);
                    if (oldStatus == null) {
                        continue;
                    }
                    if (!oldStatus.equals(status)) {
                        String indicatorErrorString = String.format(errorIndicatorFormatter, interfaceName, oldStatus, status);
                        errorTextbuilder.append(indicatorErrorString);
                        errorTextbuilder.append(",");
                    }
                }
                curInterfacesStatus.put(interfaceName, status);
            }

            if (errorTextbuilder.length() > 0) {
                String errorText = errorTextbuilder.substring(0, errorTextbuilder.length() - 1);
                String errorItem = String.format(errorMsgFormatter, identifyTitle, errorText);
                errorMsg.add(errorItem);
            }

        }

        if (curInterfacesStatus.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.ALARMING);
            resultInfo.addIndicator("Exception", "no useful info got from result of command " +
                    "\"show port-channel summary\"");
            errorMsg.add("no useful info got from result of command \"show port-channel summary\"");
            sendDetectResult(resultInfo);
            return errorMsg;
        }

        if(errorMsg.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
        } else {
            resultInfo.setLevel(AlarmLevel.ALARMING);
        }

        saveCurrentIndicators(curInterfacesStatus, resultInfo.getIpAddr());

        sendDetectResult(resultInfo);


        return errorMsg;
    }

    /**
     * 更新接口状态信息
     * @param curInterfacesStatus
     */
    private void saveCurrentIndicators(Map<String, String> curInterfacesStatus, String ip) {
        RestHighLevelClient client = null;
        try {

            XContentBuilder xbuilder = XContentFactory.jsonBuilder().mapContents(curInterfacesStatus);

            String key = String.join("_", hostName, ip);
            client = ElasticSearchPoolUtil.getClient();

            GetRequest getRequest = new GetRequest("cisco_port_channel_summery", key);
            if (client.exists(getRequest, RequestOptions.DEFAULT)) {
                UpdateRequest updateRequest = new UpdateRequest("cisco_port_channel_summery", key).doc(xbuilder);
                client.update(updateRequest, RequestOptions.DEFAULT);
            } else {
                IndexRequest request = new IndexRequest("cisco_port_channel_summery").id(key).source(xbuilder).timeout(
                        TimeValue.timeValueSeconds(3));
                client.index(request, RequestOptions.DEFAULT);
            }

        } catch (Exception e) {
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }

    }

    /**
     * 获取上次抓取的接口状态信息
     * @return
     */
    private Map<String, String> getHistoryIndicators(String ip) {
        RestHighLevelClient client = null;
        try {
            String key = String.join("_", hostName, ip);
            client = ElasticSearchPoolUtil.getClient();
            GetRequest getRequest = new GetRequest("cisco_port_channel_summery", key);
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            Asserts.check(exists, "");
            Map<String, String> interfaceStatus = new HashMap<>();
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            response.getFields().forEach((fieldName, fieldValue) -> {
                String status = fieldValue.getValue().toString();
                interfaceStatus.put(fieldName, status);
            });
            return interfaceStatus;
        } catch (Exception e) {
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }
        return null;
    }
}
