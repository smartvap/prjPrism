package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.CiscoMeticDetectTask;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

/**
 * 异常结果解析器
 * @author zhangdatong
 * @date 2021/06/09 13:48
 * @version 1.0.0
 */
@AnalyzerName("EXCEPTION_ANALYZER")
public class ExceptionAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(ExceptionAnalyzer.class);


    public ExceptionAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    @Override
    public List<String> analysisResult() throws Exception {
        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        Exception exception = (Exception) detectResult;
        String resultDetail = buildDetailResult(exception);
        result.setDetailResult(resultDetail);
        String exceptionMsg = exception.getMessage();
        result.addIndicator("Exception", exceptionMsg);
        result.setLevel(AlarmLevel.ALARMING);
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
            logger.error("CISCO异常探测结果组织失败，执行命令为： {} 参数为：{}", command, argument, e);
        } catch (Exception e) {
            logger.error("CISCO异常探测结果上报失败，执行命令为： {} 参数为：{}", command, argument, e);
        }
        return null;
    }
}
