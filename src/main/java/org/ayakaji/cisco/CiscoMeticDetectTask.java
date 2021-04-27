package org.ayakaji.cisco;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.scheduling.DefaultCommonTask;
import org.ini4j.Ini;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

public class CiscoMeticDetectTask extends DefaultCommonTask {

    private static transient Logger logger = LogManager.getLogger(CiscoMeticDetectTask.class);

    private TelnetClient tc;

    /**
     * 预执行程序，加载配置文件并打通远程telnet连接
     * @param context
     * @throws Exception
     */
    @Override
    public void preExecute(JobExecutionContext context) throws Exception {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String ciscoHostName = dataMap.getString("hostname");
        logger.info("begin to detect CiscoMetic {}", ciscoHostName);
        String iniFilePath = dataMap.getString("iniFile");
        File iniFile = new File(iniFilePath);
        if (!iniFile.exists()) {
            logger.error("can not find config ini file named of {}, exit", iniFilePath);
            try {
                context.getScheduler().interrupt(context.getJobDetail().getKey());
            } catch (Exception e){
                logger.error("error in interrupting this task", e);
            }
        }
        Ini iniReader = new Ini(iniFile);
        Map<String, String> hostConfig = iniReader.get(ciscoHostName);
        String ip = hostConfig.get("mgr_ip");
        String port = hostConfig.get("mgr_port");
        String telnetAccount = hostConfig.get("acct");
        String telnetPwd = hostConfig.get("pass");

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (_interrupted) {
            throw new JobExecutionException("can not execute the detect because of interrupted " + _interruptReason);
        }

    }

    @Override
    public void executed(JobExecutionContext context, JobExecutionException exception) {

    }

    /**
     * Read input stream until encountered prompt. <Bug> In split-screen mode, the
     * split-screen prompt "--More--" will cause garbled characters. After deleting
     * this prompt, the effect is not good. Disable the split screen mode through
     * configure term len 0, but it will cause a potential command stuck risk, which
     * will cause a backlog of the telnet process. </Bug>
     *
     * @param is
     * @param os
     * @param pattern
     * @return
     * @throws IOException
     */
    private static String readUntil(InputStream is, PrintStream os, String pattern) throws IOException {
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuffer sb = new StringBuffer();
        char ch = (char) is.read();
        while (true) {
            sb.append(ch);
            if (ch == lastChar) {
                if (sb.toString().endsWith(pattern)) {
                    byte[] temp = sb.toString().getBytes("iso8859-1");
                    return new String(temp, "GBK");
                }
            } else if (ch == '-') { // try to remove split-screen prompt
                if (sb.toString().endsWith("--More--")) {
                    sb.delete(sb.length() - 14, sb.length());
                    write(os, ' ');
                }
            }
            ch = (char) is.read();
        }
    }

    private static void write(PrintStream os, char value) {
        os.print(value);
        os.flush();
    }

    private static void write(PrintStream os, String value) {
        os.println(value);
        os.flush();
    }
}
