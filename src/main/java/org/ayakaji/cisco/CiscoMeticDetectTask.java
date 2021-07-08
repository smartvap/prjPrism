package org.ayakaji.cisco;

import io.netty.util.internal.StringUtil;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.cisco.analyzers.AnalyzerFactory;
import org.ayakaji.scheduling.DefaultCommonTask;
import org.ayakaji.util.IniConfigFactory;
import org.ayakaji.util.ThreadPool;
import org.ini4j.Profile;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CiscoMeticDetectTask extends DefaultCommonTask {

    private static transient final Logger logger = LogManager.getLogger(CiscoMeticDetectTask.class);

    private TelnetClient client;

    private String charset;

    private String hostname;

    private String ip;

    private Map<String, Object> cmdExecuteResult;

    private List<String> alarmInfo;


    /**
     * 预执行程序，加载配置文件、打通远程telnet连接并登录
     *
     * @param context 任务上下文
     * @throws Exception 异常信息
     */
    @Override
    public void preExecute(JobExecutionContext context) throws Exception {

//        初始化cmdExecuteResult用来记录执行结果
        cmdExecuteResult = new HashMap<>();
//        初始化alarmInfo用来记录告警信息
        alarmInfo = new ArrayList<>();

        JobDataMap dataMap = context.getMergedJobDataMap();
        hostname = dataMap.getString("hostname");

        logger.info("begin to detect CiscoMetic {}", hostname);
        Profile.Section hostConfig = IniConfigFactory.getHostConfigSection(hostname);
        ip = hostConfig.get("mgr_ip");
        String portStr = hostConfig.get("mgr_port");
        int port = Integer.valueOf(portStr);
        String telnetAccount = hostConfig.get("acct");
        String telnetPwd = hostConfig.get("pass");
        charset = hostConfig.get("charset");
        String errorFlag = hostConfig.get("loginErrorFlag");
        int timeoutLimit = 5000;
        try {
            timeoutLimit = dataMap.getIntValue("timeoutLimit");
        } catch (Exception e) {
            logger.warn("can not read the connect timeout limit param from task config named \"timeoutLimit\", "
                    + " use default config {} in code", timeoutLimit);
        }
//        创建telnet连接并使用用户名和密码登录
        client = new TelnetClient("ansi");
        client.setConnectTimeout(timeoutLimit);
        client.connect(ip, port);
        waitForInput("login:");
        send(telnetAccount);
        waitForInput("Password:");
        send(telnetPwd);
        String[] loginResult = readLines();
        checkLoginResult(loginResult, errorFlag);
        checkMultiLoginUsers();
    }

    private void checkMultiLoginUsers() throws Exception {
        executeCommand("show users");
        String[] users = readLines("#");
        if (users.length > 1) {
            throw new InterruptedException("there are more than one user linked to this switch, exit");
        }
    }

    /**
     * 检查登录是否成功
     *
     * @param loginResult 登录返回报文
     * @param errorFlag   登录失败标志信息
     * @throws InterruptedException 登录失败直接抛出打断异常，中断后续运行
     */
    private void checkLoginResult(String[] loginResult, String errorFlag) throws InterruptedException {
        boolean isError = Arrays.stream(loginResult).anyMatch(item -> {
            return item.contains(errorFlag);
        });
        if (isError) {
            throw new InterruptedException("login failed by return msg" + String.join("\r\n", loginResult));
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap params = context.getMergedJobDataMap();
        String commandStrs = params.getString("required detect cmds");
        if (StringUtil.isNullOrEmpty(commandStrs)) {
            throw new JobExecutionException("nothing to do");
        }
        String[] commands = commandStrs.split(",");

        for (int i = 0; i < commands.length; i++) {
            String cmd = commands[i];
            if (StringUtil.isNullOrEmpty(cmd.trim())) {
                logger.warn("the command to execute is empty, skip");
                return;
            }
            executeCommand(cmd);
//            每个命令执行间隔1秒，避免任务执行过快
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                logger.error("task interrupted");
                throw new JobExecutionException(e);
            }
        }

    }

    /**
     * 执行命令，命令的格式一般为一条完敕的命令，如果有参数，需要使用[]将参数的名称括起来，如果参数有多种，需要使用[arg1|arg2……]形式进行标示
     *
     * @param cmd 命令的标题，不包括[arg1|……]部分
     */
    private void executeCommand(String cmd) {
        String cmdName = cmd;
        String[] args = null;
        if (cmd.contains("[")) {
            int lf = cmd.indexOf("[");
            int rf = cmd.indexOf("]");
            cmdName = cmd.substring(0, lf);
            String argsStr = cmd.substring(lf + 1, rf);
            args = argsStr.split("|");
        }
        String cmdForExecute = IniConfigFactory.getHostConfig(hostname, cmdName);
        if (StringUtil.isNullOrEmpty(cmdForExecute)) {
            cmdForExecute = IniConfigFactory.getCommonCmd(cmdName);
        }
        if (StringUtil.isNullOrEmpty(cmdForExecute)) {
            Exception noSuchCmd = new IllegalArgumentException("no command find by name " + cmdName);
            cmdExecuteResult.put(cmd, noSuchCmd);
            return;
        }
        if (args == null) {     //如果没有参数项，直接执行命令
            send(cmdForExecute);
            getAndRecordCmdExecuteResult(cmdForExecute);
        } else {        //如果有参数项，依次加入参数执行
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                String realCmd = String.join(" ", cmdForExecute, argument);
                send(realCmd);
                getAndRecordCmdExecuteResult(String.format(cmd + " [%s]", argument));
            }
        }

    }

    /**
     * 根据执行命令的标题，查看结果
     *
     * @param cmd 执行命令的标题
     */
    private void getAndRecordCmdExecuteResult(String cmd) {
        try {
            String[] executeResult = readLines("#");
            cmdExecuteResult.put(cmd, executeResult);
        } catch (IOException e) {
            logger.error("Exception occured while executing command {} of host {}", cmd, hostname, e);
            cmdExecuteResult.put(cmd, e);
        }
    }


    @Override
    public void executed(JobExecutionContext context, JobExecutionException exception) {
        /**
         * 先关闭telnet连接
         */
        try {
            executeCommand("exit");
            client.disconnect();
        } catch (IOException e) {
            client = null;
            logger.error("error in close telnet connection while detecting CISCO host {}", hostname, e);
        }
        Date detectTime = context.getFireTime();
        if (exception != null) {
            String deteckKey = context.getTrigger().getKey().getName();
            String detectGroup = context.getTrigger().getKey().getGroup();
            logger.error("error occured while executing detect task of {}.{}", detectGroup, deteckKey, exception);
        }
        List<Future<List<String>>> futures = new ArrayList<Future<List<String>>>();
//        调用解析器解析监测结果信息
        for (Map.Entry<String, Object> excutedResult : cmdExecuteResult.entrySet()) {
            String cmd = excutedResult.getKey();
            Object result = excutedResult.getValue();
            String cmdName = null;
            String argument = null;
            if (cmd.contains("[")) {
                int lf = cmd.indexOf("[");
                int rf = cmd.indexOf("]");
                cmdName = cmd.substring(lf);
                argument = cmd.substring(lf + 1, rf);
            } else {
                cmdName = cmd;
            }

            final String executedCmd = cmdName;
            final String args = argument;


            try {
                Future<List<String>> anaLyzeFuture = ThreadPool.submit(AnalyzerFactory.getAnalyzer(cmdName, hostname,
                        executedCmd, args, result, detectTime));
                futures.add(anaLyzeFuture);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                    IllegalAccessException e) {
                continue;
            }
        }
        for (Future<List<String>> future : futures) {
            List<String> errorList = null;
            try {
                errorList = future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("error in collection analyze result", e);
            }
            if (errorList != null && !errorList.isEmpty()) {
                alarmInfo.addAll(errorList);
            }
        }
        if (!alarmInfo.isEmpty()) {
            logger.warn("CISCO监控指标出现异常，异常信息：\n {}", String.join("\n", alarmInfo));
        }
    }

    /**
     * 按行读取命令返回结果，有标记需要返回的特殊字符
     *
     * @param expectedMsg
     * @return 返回的结果行
     * @throws IOException
     */
    private String[] readLines(String expectedMsg) throws IOException {
        String[] lines = null;
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = client.getInputStream(); //读取命令的流
        InputStreamReader reader = new InputStreamReader(inputStream, charset);
        char[] buf = new char[1000];
        for (int size = reader.read(buf); size > 0; size = reader.read(buf)) {
            String item = new String(Arrays.copyOf(buf, size));
            builder.append(item);
            if (expectedMsg == null && size < buf.length) {
                break;
            }
            if (item.trim().endsWith(expectedMsg)) {
                break;
            }
        }
        lines = builder.toString().split("\r\n");
        lines = Arrays.copyOf(lines, lines.length - 1);
        return lines;
    }

    /**
     * 读取命令执行结果信息（只要读完了就返回）
     *
     * @return 命令执行结果行数组
     * @throws IOException
     */
    private String[] readLines() throws IOException {
        String[] lines = null;
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = client.getInputStream(); //读取命令的流
        InputStreamReader reader = new InputStreamReader(inputStream, charset);
        char[] buf = new char[1000];
        for (int size = reader.read(buf); size > 0; size = reader.read(buf)) {
            String item = new String(Arrays.copyOf(buf, size));
            builder.append(item);
            if (size < buf.length) {
                break;
            }
        }
        lines = builder.toString().split("\r\n");
        return lines;
    }

    /**
     * 等待执行，只有出现特有的标志才继续执行
     *
     * @param expectedMsg 特定标志
     * @throws IOException 读取异常信息
     */
    private void waitForInput(String expectedMsg) throws IOException {
        readLines(expectedMsg);
    }

    /**
     * 发送命令
     *
     * @param command 命令
     */
    private void send(String command) {
        OutputStream os = client.getOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.print(command);
        ps.print("\r\n");
        ps.flush();
    }

}
