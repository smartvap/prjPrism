package org.ayakaji.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.ayakaji.pojo.AlarmLevel;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionListenerResponseHandler;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.ml.PostDataRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Plugin(name = "ESLogAppender", category = "Core", elementType = "appender", printObject = true)
public class ElasticsearchLogAppender extends AbstractAppender {

    GenericService genericService =null;
    //dubbo日志方法
    private String logMethodName = "saveLog";

    private String hostName = System.getProperty("hostname");

    private static String HOST_NAME;
    private static String IP_ADDR;

    static {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            HOST_NAME = localhost.getHostName();
            IP_ADDR = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.error("can not get host info of this client", e);
            HOST_NAME = "UNKNOWN";
            IP_ADDR = "UNKNOWN";
        }
    }

    protected ElasticsearchLogAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                                       boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        genericService = new GenericService();
    }

    @PluginFactory
    public static ElasticsearchLogAppender createAppender(
            @PluginAttribute("name") String name, @PluginElement("Filter") final Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        return new ElasticsearchLogAppender(name, filter, layout, ignoreExceptions);
    }

    /**
     * 记录日志的方法，保存日志信息，形成日志信息对象，调用日志上报类上报日志信息
     * @param event 日志事件对象
     */
    @Override
    public void append(LogEvent event) {
        event.getLevel();
        LogInfo log = new LogInfo(event);
        try {
            genericService.doLog(log);
        } catch (Exception e) {
            LOGGER.error("ERROR IN REPORTING LogInfo {}", log, e);
        }
    }



    /**
     * 日志上报类
     */
    class GenericService {

        /**
         * 日志上报方法，调取Elasticsearch连接，将上报日志进行上传
         * @param log 转化后的上报日志对象
         */
        public void doLog(LogInfo log) throws Exception {
//            构建要放入到Elasticsearch中的日志对象
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("hostName", HOST_NAME);
                builder.field("ipAddr", IP_ADDR);
                builder.field("level", log.level);
                builder.field("occurTime", log.occurTime);
                builder.field("message", log.message);
                builder.field("messageDetail", log.messageDetail);
            }
            builder.endObject();
            IndexRequest request = new IndexRequest("detector_running_log").id(String.valueOf(log.occurTime.getTime()))
                    .source(builder).timeout(TimeValue.timeValueSeconds(1));
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            client.index(request, RequestOptions.DEFAULT);
            ElasticSearchPoolUtil.returnClient(client);
        }

    }

    /**
     * 日志上报对象
     */
    class LogInfo {

        private AlarmLevel level;   //日志等级INFO/WARNING/ALARMING
        private Date occurTime;     //发生时间
        private String message;     //日志主要信息
        private String messageDetail;   //日志明细信息(异常时是异常堆栈信息，如果是正常日志信息，则与MESSAGE字段信息一致)

        public LogInfo(LogEvent event) {
            Level level = event.getLevel();
            Throwable exception = event.getThrown();
            AlarmLevel alarmLevel = AlarmLevelTransfer.translate(level, exception != null);
            Date time = new Date(event.getTimeMillis());
            String message = event.getMessage().getFormattedMessage();
            this.level = alarmLevel;
            this.occurTime = time;
            this.message = message;
            if (exception == null) {
                this.messageDetail = message;
            } else {    //如果有异常信息，则要取出完整的堆栈信息并
                StringWriter writer = new StringWriter();
                PrintWriter printer = new PrintWriter(writer);
                exception.printStackTrace(printer);
                this.messageDetail = writer.toString();
            }

        }

        public LogInfo(Date occurTime, String message) {
            this.occurTime = occurTime;
            this.message = message;
            this.level = AlarmLevel.INFO;
            this.messageDetail = message;
        }

        public LogInfo(AlarmLevel level, Date occurTime, String message) {
            this(occurTime, message);
            this.level = level;
        }

//        Getters & Setters

        public String getHostName() {
            return hostName;
        }

        public AlarmLevel getLevel() {
            return level;
        }

        public void setLevel(AlarmLevel level) {
            this.level = level;
        }

        public Date getOccurTime() {
            return occurTime;
        }

        public void setOccurTime(Date occurTime) {
            this.occurTime = occurTime;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessageDetail() {
            return messageDetail;
        }

        public void setMessageDetail(String messageDetail) {
            this.messageDetail = messageDetail;
        }


    }

    /**
     * 日志级别转换类，将日志级别转化为系统告警级别
     */
    static class AlarmLevelTransfer {


        /**
         * 转换方法, 日志级别仅支持INFO, WARN, ERROR 三种日志级别的明确转换，分别对应告警类别中的INFO,WARNING,ALARMING，
         * 其余类型转为UNKNOWN 如果没有标明可识别日志级别但有异常信息，则定义为WARNNING
         * @param level 日志级别
         * @param hasException 有异常信息
         * @return AlarmLevel 告警级别
         */
        public static AlarmLevel translate(Level level, boolean hasException) {

            switch (level.getStandardLevel()) {
                case INFO: return AlarmLevel.INFO;
                case WARN: return AlarmLevel.WARNING;
                case ERROR: return AlarmLevel.ALARMING;
                default:
                    if (hasException) {
                        return AlarmLevel.WARNING;
                    }
                    return AlarmLevel.UNKNOWN;
            }

        }
    }
}
