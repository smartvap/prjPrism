package org.ayakaji.cisco.analyzers;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.CiscoMeticDetectTask;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 结果分析公共接口
 * @author zhangdatong
 * @date 2021/06/08 10:10
 */
public abstract class ResultAnalyzer implements Callable<List<String>> {

    private static transient Logger logger = LogManager.getLogger(ResultAnalyzer.class);

    protected String hostName;
    protected String command;
    protected String argument;
    protected Object detectResult;
    protected Date detectTime;


    public ResultAnalyzer (String hostName, String command, String argument, Object detectResult, Date detectTime) {
        this.hostName = hostName;
        this.command = command;
        this.argument = argument;
        this.detectResult = detectResult;
        this.detectTime = detectTime;
    }

    public abstract List<String> analysisResult() throws Exception;

    @Override
    public List<String> call() throws Exception {
        return analysisResult();
    }

    protected void buildXContentBuilder(XContentBuilder builder, DetectResult result) throws IOException {
        builder.startObject();
        builder.field("host", result.getHost());
        builder.field("ipAddr", result.getIpAddr());
        builder.field("command", result.getCommand());
        builder.field("arguments", result.getArgument());
        builder.field("realCommand", result.getRealCommand());
        builder.field("level", result.getLevel().name());
        builder.field("detectTime", result.getDetectTime());
        builder.field("detailResult", result.getDetailResult());
        builder.startObject("indicators");
        result.getIndicators().forEach((indicator, detectResult) -> {
            try {
                builder.field(indicator, detectResult);
            } catch (IOException e) {
                return;
            }
        });
        builder.endObject();
        builder.endObject();
    }

    /**
     * 发送探测结果信息
     *
     * @param result 探测结果信息对象
     */
    protected void sendDetectResult(DetectResult result) {
        try {
            XContentBuilder xbuilder = XContentFactory.jsonBuilder();
            buildXContentBuilder(xbuilder, result);
            String key = result.getHost().toLowerCase() + "_" + result.getCommand().toLowerCase() + "_"
                    + result.getDetectTime().getTime();
            IndexRequest request = new IndexRequest("cisco_detect_result").id(key).source(xbuilder).
                    timeout(TimeValue.timeValueSeconds(3));
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            client.index(request, RequestOptions.DEFAULT);
            ElasticSearchPoolUtil.returnClient(client);
        } catch (IOException e) {
            logger.error("CISCO 探测结果组织失败，执行命令为： {} 参数为：{}", result.getCommand(),
                    result.getArgument(), e);
        } catch (Exception e) {
            logger.error("CISCO 探测结果上报失败，执行命令为： {} 参数为：{}", result.getCommand(),
                    result.getArgument(), e);
        }
    }

    /**
     * 当命令执行结果不是期望的结果时，组织非期望结果异常
     *
     * @param command  执行的命令
     * @param argument 执行命令的参数
     * @param lines    返回的结果
     * @return 非期望结果异常
     */
    protected Exception buildUnExceptedResultException(String command, String argument, String[] lines) {
        Exception exception = new Exception("未找到期望的结果");
        StackTraceElement[] stackTraces = new StackTraceElement[lines.length];
        for (int i = 0; i < lines.length; i++) {
            StackTraceElement stackTrace = new StackTraceElement(command, argument, lines[i],
                    i + 1);
        }
        exception.setStackTrace(stackTraces);
        return exception;
    }

    /**
     * 解析结果文本中的指标信息
     *
     * @param context 一整行的命令结果信息
     * @return 有效信息的属性数组
     */
    protected List<String> parseProperties(String context) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++)
                    ;
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2) {
                    builder.append(curChar);
                } else {
                    String property = builder.toString().trim();
                    properties.add(property);
                    builder = new StringBuilder();
                }
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }

    /**
     * 严格的解析结果文本中的指标信息，只要发现有空格就分隔
     *
     * @param context 一整行的命令结果信息
     * @return 有效信息的属性数组
     */
    protected List<String> strictParseProperties(String context) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++)
                    ;
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                String property = builder.toString().trim();
                properties.add(property);
                builder = new StringBuilder();
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }

    /**
     * 带参分割字段方法，如果达到了字段限制，则将后面的字段登合并为一个字段
     * @param context   正文
     * @param colNum    字段数限制
     * @return
     */
    protected List<String> parseProperties(String context, int colNum) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0, curIdx = 1; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2 || curIdx >= colNum) {
                    builder.append(curChar);
                } else {
                    String property = builder.toString().trim();
                    properties.add(property);
                    builder = new StringBuilder();
                    curIdx++;
                    continue;
                }
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }

    /**
     * 将异常堆栈信息打印成字符串并返回
     * @param exception
     * @return String
     */
    protected String buildDetailResult(Exception exception) {
        StringWriter stackWriter = new StringWriter();
        PrintWriter exceptionPrinter = new PrintWriter(stackWriter);
        exception.printStackTrace(exceptionPrinter);
        String resultDetail = stackWriter.toString();
        return resultDetail;
    }

    /**
     * 将执行结果合并成一个字符串返回
     * @param lines
     * @return
     */
    protected String buildDetailResult(String[] lines) {
        String resultDetail = String.join("\n", lines);
        return resultDetail;
    }

    /**
     * 格式化行，用作与历史数据进行比对，尽可能地减少空格和制表符，但是保留空行，以方便比对
     * @param original
     * @return
     */
    protected String formatLine(String original) {
        char[] lineChars = original.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2) {
                    builder.append(curChar);
                }
            } else {
                builder.append(curChar);
            }
        }
        return builder.toString();
    }


}
