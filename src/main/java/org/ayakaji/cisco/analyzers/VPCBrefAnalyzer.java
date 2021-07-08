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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * show vpc brief命令解析器
 * @author zhangdatong
 *  date 2021/06/21 10:02
 */
@AnalyzerName("SHOW_VPC_BRIEF")
public class VPCBrefAnalyzer extends ResultAnalyzer{

    public VPCBrefAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }


    /**
     * show vpc brief命令解析,与历史数据进行比对，有一行不匹配则进行告警,同时将当前的命令执行结果更新到ES中，替换掉原来的结果
     * @return errorMsg
     * @throws Exception 执行抛出的异常信息
     */
    @Override
    public List<String> analysisResult() throws Exception {
        List<String> errorMsg = new ArrayList<>();

        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        resultInfo.setDetailResult(resultDetail);

        if (lines == null || lines.length == 0) {
            errorMsg.add("command show vpc brief has nothing as execute result");
            resultInfo.setLevel(AlarmLevel.ALARMING);
            resultInfo.addIndicator("Exception", "command show vpc brief has nothing as execute result");
            sendDetectResult(resultInfo);
            return errorMsg;
        }

        List<String> formattedLines = Arrays.stream(lines).map(this::formatLine).collect(Collectors.toList());

//       获取历史数据
        List<String> historyLines = getHistoryResult(resultInfo.getIpAddr());
        List<String> formattedHisLines = historyLines.stream().map(this::formatLine).collect(Collectors.toList());

//        如果没有历史数据，则证明这是第一次探测到这一指标，再比对就没有了意义，所以直接返回一个空的异常信息并把本次的信息记录到ES中
        if (historyLines == null || historyLines.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
            saveCurrentResult(lines, resultInfo);
            return errorMsg;
        }

        String errorFormat = "line at No.%d of current result is different from history, since [%s], current [%s]";

//        开始比对
        for (int i = 0, formattedSize = formattedLines.size(), historySize = formattedHisLines.size(),
             len = Math.max(formattedSize, historySize); i < len; i++) {
            String curLine = formattedSize > i ? formattedLines.get(i) : "Empty";
            String historyLine = historySize > i ? formattedHisLines.get(i) : "Empty";
            if (!curLine.equals(historyLine)) {
                String curReal = formattedSize > i ? lines[i] : "Empty";
                String hisReal = historySize > i ? historyLines.get(i) : "Empty";
                String errorItem = String.format(errorFormat, i, curReal, hisReal);
                errorMsg.add(errorItem);
                resultInfo.addIndicator(String.valueOf(i), errorItem);
            }
        }

        if (errorMsg.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
        } else {
            resultInfo.setLevel(AlarmLevel.ALARMING);
            saveCurrentResult(lines, resultInfo);
            return errorMsg;
        }

        sendDetectResult(resultInfo);

        return errorMsg;
    }

    /**
     * 保存当前的信息
     * @param lines
     * @param resultInfo
     */
    private void saveCurrentResult(String[] lines, DetectResult resultInfo) {
        String key = String.join("_", hostName, resultInfo.getIpAddr());

        RestHighLevelClient client = null;
        try {
            XContentBuilder xbuilder = XContentFactory.jsonBuilder().array("record",
                    lines);
            GetRequest getRequest = new GetRequest("cisco_vpc_bref", key);
            client = ElasticSearchPoolUtil.getClient();
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            if (exists) {
                UpdateRequest updateRequest = new UpdateRequest("cisco_vpc_bref", key).doc(xbuilder);
                client.update(updateRequest, RequestOptions.DEFAULT);
            } else {
                IndexRequest request = new IndexRequest("cisco_vpc_bref").id(key).source(xbuilder).timeout(
                        TimeValue.timeValueSeconds(3));
                client.index(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }

        sendDetectResult(resultInfo);
    }

    /**
     * 获取历史信息
     * @return
     */
    private List<String> getHistoryResult(String ip) {
        RestHighLevelClient client = null;
        try {
            String key = String.join("_", hostName, ip);
            client = ElasticSearchPoolUtil.getClient();
            GetRequest getRequest = new GetRequest("cisco_vpc_bref", key);
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            Asserts.check(exists, "");
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            List<String> historyRecord = response.getField("record").getValues().stream().map(item -> (String) item)
                    .collect(Collectors.toList());
            return historyRecord;
        } catch (Exception e) {
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }
        return null;
    }


}
