package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.ayakaji.pojo.AlarmLevel;
import org.ayakaji.util.IniConfigFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * cdp neighbors 命令结果分析器
 * @author zhangdatong
 * @date 2021/06/08 17:38
 * @version 1.0.0
 */
@AnalyzerName("CDP_NEIGHBORS")
public class CDPNeighborsAnalyzer extends ResultAnalyzer {

    private static transient Logger logger = LogManager.getLogger(CDPNeighborsAnalyzer.class);

    public CDPNeighborsAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show cdp neighbors detail命令执行结果分析及处理，保存neighbors探测结果信息和探测日志信息，同步更新CISCO neighbors信息
     * @return
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {

        String readFlag = "Device-ID";
        List<String[]> items = new ArrayList<String[]>();
        boolean beginRead = false;
        String[] neighborsStrArr = (String[]) detectResult;

        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        result.setDetailResult(buildDetailResult(neighborsStrArr));

        /**
         * 解析命令执行结束，获取neighbors指标信息
         */
        for (int i = 0; i < neighborsStrArr.length; i++) {
            String line = neighborsStrArr[i];
            if (line.contains(readFlag)) {
                beginRead = true;
            }
            if (!beginRead) {
                continue;
            }
            List<String> properties = parseProperties(line);
            items.add(properties.toArray(new String[properties.size()]));
        }
//                如果分析的结果是空，证明没有找到期望的指标信息，记录日志并返回
        if (items.isEmpty()) {
            Exception unExceptedResultException = buildUnExceptedResultException(command, argument, neighborsStrArr);
            logger.error("CISCO 探测结果分析异常，执行命令 {} ，参数 {} 未找到期望的指标信息", command, argument,
                    unExceptedResultException);

            result.setLevel(AlarmLevel.ALARMING);
            result.addIndicator("Exception", "未找到期望的指标信息");
            sendDetectResult(result);
            return null;
        }
        String key = hostName + "_" + detectTime.getTime();
        try {
            XContentBuilder xbuilder = XContentFactory.jsonBuilder();
            xbuilder.startObject();
            xbuilder.field("hostName", hostName);
            xbuilder.field("detectTime", detectTime);
            String ipAddr = IniConfigFactory.getHostConfigSection(hostName).get("mgr_ip");
            xbuilder.field("ipAddr", ipAddr);
            xbuilder.startArray("cdpNeighbors");
            String[] titles = items.get(0);
            for (int i = 0; i < titles.length; i++) {
                titles[i] = titles[i].trim().replaceAll(" ", "_").toLowerCase();
            }
            for (int i = 1, len = items.size(); i < len; i++) {
                String[] item = items.get(i);
                xbuilder.startObject();
                for (int j = 0; j < item.length; j++) {
                    xbuilder.field(titles[j], item[j]);
                }
                xbuilder.endObject();
            }
            xbuilder.endArray();
            xbuilder.endObject();

            IndexRequest request = new IndexRequest("cisco_neighbors_detail").id(key).source(xbuilder).
                    timeout(TimeValue.timeValueSeconds(3));
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            client.index(request, RequestOptions.DEFAULT);
            ElasticSearchPoolUtil.returnClient(client);
        } catch (Exception e) {
            logger.error("CISCO探测结果上报失败，执行命令为： {} 参数为：{}", command, argument, e);
            return null;
        }
        result.addIndicator("cdpNeighborsKey", key);
        result.setLevel(AlarmLevel.INFO);
        sendDetectResult(result);
        updateOrCreateNeighborsInfo(hostName, result.getIpAddr(), items);
        return null;
    }

    /**
     * 查看是否存在该CISCO交换机的neighbors信息，如果存在，就更新，如果不存在，就新增一条
     * @param host CISCO主机名
     * @param ip CISCO的IP
     * @param items neighbors信息
     */
    private void updateOrCreateNeighborsInfo(String host, String ip, List<String[]> items) {
        if (items.isEmpty()) {
            return;
        }
        try {
            XContentBuilder xbuilder = XContentFactory.jsonBuilder();
            xbuilder.startObject();
            xbuilder.field("hostName", host);
            String ipAddr = IniConfigFactory.getHostConfigSection(host).get("mgr_ip");
            xbuilder.field("ipAddr", ipAddr);
            xbuilder.startArray("cdpNeighbors");
            String[] titles = items.get(0);
            for (int i = 0; i < titles.length; i++) {
                titles[i] = titles[i].trim().replaceAll(" ", "_").toLowerCase();
            }
            for (int i = 1, len = items.size(); i < len; i++) {
                String[] item = items.get(i);
                xbuilder.startObject();
                for (int j = 0; j < item.length; j++) {
                    xbuilder.field(titles[j], item[j]);
                }
                xbuilder.endObject();
            }
            xbuilder.endArray();
            xbuilder.endObject();
            String key = String.join("_", host, ip);
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            GetRequest getRequest = new GetRequest("cisco_cdp_neighbors", key);
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            if (exists) {
                UpdateRequest updateRequest = new UpdateRequest("cisco_cdp_neighbors", key).doc(xbuilder);
                client.update(updateRequest, RequestOptions.DEFAULT);
            } else {
                IndexRequest request = new IndexRequest("cisco_cdp_neighbors").id(key).source(xbuilder).timeout(
                        TimeValue.timeValueSeconds(3));
                client.index(request, RequestOptions.DEFAULT);
            }
            ElasticSearchPoolUtil.returnClient(client);
        } catch (Exception e) {
            logger.error("更新CISCO neighbors 失败，主机为： {} ip为：{}", host, ip, e);
        }
    }
}
