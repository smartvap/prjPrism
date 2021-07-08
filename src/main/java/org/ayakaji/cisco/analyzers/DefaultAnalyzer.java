package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.ayakaji.pojo.AlarmLevel;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 默认结果解析器，不参加告警，只保存日志信息
 * @author zhangdatong
 * @date 2021/06/08 10:17
 */
@AnalyzerName("UNKNOWN")
public class DefaultAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(DefaultAnalyzer.class);

    public DefaultAnalyzer (String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }


    @Override
    public List<String> analysisResult() {
        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        String[] resultLines = (String[]) detectResult;
        String resultDetail = buildDetailResult(resultLines);
        result.addIndicator("WARNNING", "No analyzer for this command");
        result.setDetailResult(resultDetail);
        result.setLevel(AlarmLevel.WARNING);
        try {
            XContentBuilder xbuilder = XContentFactory.jsonBuilder();
            buildXContentBuilder(xbuilder, result);
            String key = hostName.toLowerCase() + "_" + command.toLowerCase() + "_" + detectTime.getTime();
            IndexRequest request = new IndexRequest("cisco_detect_result").id(key).source(xbuilder).
                    timeout(TimeValue.timeValueSeconds(3));
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            client.index(request, RequestOptions.DEFAULT);
            ElasticSearchPoolUtil.returnClient(client);
        } catch (IOException e) {
            logger.error("CISCO不可解析探测结果组织失败，执行命令为： {} 参数为：{}", command, argument, e);
        } catch (Exception e) {
            logger.error("CISCO不可解析探测结果上报失败，执行命令为： {} 参数为：{}", command, argument, e);
        }
        return null;
    }
}
