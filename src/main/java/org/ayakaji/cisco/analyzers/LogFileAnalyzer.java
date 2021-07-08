package org.ayakaji.cisco.analyzers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Show logging logfile命令执行结果执行器
 * @author zhangdatong
 * @date 2021/06/10 9:13
 * @version 1.0.0
 */
@AnalyzerName("SHOW_LOGGING_LOGFILE")
public class LogFileAnalyzer extends ResultAnalyzer{

    private static transient Logger logger = LogManager.getLogger(LogFileAnalyzer.class);

    private static volatile FileTime updateTime;    //配置文件更新时间

    private static volatile String[] filters;   //配置文件提供的筛选条件


    static {
//        先看有没有自定义的筛选条件
        Path configPath = Paths.get("logFilter.conf");
        boolean customConfigExists = Files.exists(configPath);
        /**
         * 如果自定义的筛选条件配置文件存在的话，则取自定义配置文件的修改时间为条件更新时间，自定义的配置文件中的筛选条件为筛选条件
         */
        if (customConfigExists) {
            logger.debug("find custom filter config file, try to load the coustom configurations");
            try {
                updateTime = Files.getLastModifiedTime(configPath);
                List<String> configLines = Files.readAllLines(configPath);
                if (configLines != null && !configLines.isEmpty()) {
                    filters = configLines.toArray(new String[configLines.size()]);
                }
            } catch (IOException e) {
            }
        } else {
            logger.debug("can not find custom filter configs use default configs as well");
            /**
             * 如果自定义的配置文件不存在，取封装在jar包中的默认的配置文件中的筛选条件作为筛选条件，取当前时间为筛选条件的更新时间
             */
            updateTime = FileTime.fromMillis(System.currentTimeMillis());
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("logFilter.conf");
            try {
                configPath = Paths.get(configURL.toURI());
                if (Files.exists(configPath)) {
                    List<String> configLines = Files.readAllLines(configPath);
                    if (configLines != null && !configLines.isEmpty()) {
                        filters = configLines.toArray(new String[configLines.size()]);
                    }
                }
            } catch (URISyntaxException | IOException e) {
            }
        }
    }

    public LogFileAnalyzer(String hostName, String command, String argument, Object detectResult, Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
        modifyFilter();
    }

    /**
     * 检查是不是更新了配置文件，如果配置文件有更新，则要更新筛选条件，同时将文件最后更新时间作为筛选条件最后更新时间
     */
    private void modifyFilter() {
        Path configPath = Paths.get("logFilter.conf");
        boolean customConfigExists = Files.exists(configPath);
        if (customConfigExists) {
            logger.debug("custom filter config file exists, try to update current filter configs in analyzer");
            try {
                FileTime lastModifiedTime = Files.getLastModifiedTime(configPath);
                if (updateTime == null || lastModifiedTime.compareTo(updateTime) > 0) {
                    logger.debug("find that the config is changed after last load, update the current config in " +
                            "analyzer indeed");
                    List<String> configLines = Files.readAllLines(configPath);
                    if (configLines != null && !configLines.isEmpty()) {
                        filters = configLines.toArray(new String[configLines.size()]);
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 分析日志结果信息，如果日志中的等级小于或等于5，且符合日志筛选条件（配置文件提供）则要加入告警信息返回
     * 日志等级：
     * 0(emergencies)          1(alerts)       2(critical)
     * 3(errors)               4(warnings)     5(notifications)
     * 6(information)          7(debugging)
     * @return 需要告警的信息
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {
        List<String> errorMsg = new ArrayList<>();
        String[] logs = (String[]) detectResult;
        String detailResult = buildDetailResult(logs);
        DetectResult result = new DetectResult(hostName, command, argument, detectTime);
        result.setDetailResult(detailResult);
        Pattern logLevelPattern = Pattern.compile("%[^-]*-([\\d]{1})-");
        for (int i = 0; i < logs.length; i++) {
            String line = logs[i];
            if (line.contains("%") && matchedFilter(line)) {
                Matcher matcher = logLevelPattern.matcher(line);
                if (matcher.find()) {
                    String levelStr = matcher.group(1);
                    int level = Integer.valueOf(levelStr);
                    if (level <= 5) {
                        errorMsg.add(line);
                    }
                }
            }
        }
        if (!errorMsg.isEmpty()) {
            String[] errorArr = errorMsg.stream().toArray(String[]::new);
            result.addIndicator("errorLog", String.join("\n", errorArr));
        } else {
            result.addIndicator("errorLog", "nothing");
        }

        sendDetectResult(result);

        return errorMsg;
    }

    /**
     * 查看日志行是否符合筛选条件
     * @param source    要检查的日志行
     * @return  是否符合筛选条件
     */
    private boolean matchedFilter(String source) {
//        如果没有筛选条件，则默认符合筛选条件（即通过）
        if (filters == null) {
            return true;
        }
        for (int i = 0; i < filters.length; i++) {
            String filter = filters[i];
            if (source.contains(filter)) {
                return true;
            }
        }
        return false;
    }
}
