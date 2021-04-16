package org.ayakaji.scheduling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.quartz.Scheduler;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;


import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 统一任务调度器，所有的定时任务（包括周期执行、立即执行和定时执行）都使用此任务调度器统一调度
 * 单例执行，全局唯一
 * @author zhangdatong
 * @version 1.0.0
 * @date 2021-04-14
 */
public class ScheduleDeamon {

    private static transient Logger logger = LogManager.getLogger(ScheduleDeamon.class);

    private Scheduler scheduler;    //quartz调度器

    private static ScheduleDeamon instance;     //调度器实例

    private ScheduleDeamon() {
        try {
            scheduler = initScheduler();
            scheduler.getListenerManager().addJobListener(new DefaultJobListener());
        } catch (Exception e) {
            logger.error("can not build task scheduler in this application exit because of kernel module error", e);
            System.exit(1);
        }
    }

    public void start() {
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            logger.error("can not run task scheduler in this application exit because of kernel module error", e);
            System.exit(1);
        }
    }

    /**
     * 初始化任务调度器参数及添加任务
     */
    private Scheduler initScheduler() throws DocumentException, SchedulerException {

//        加载配置文件
        URL configFileUrl = this.getClass().getClassLoader().getResource("taskconfig.xml");

        SAXReader configReader = new SAXReader();
        Document configDoc = configReader.read(configFileUrl);
        Element root = configDoc.getRootElement();
        Element schedulerConfig = root.element("scheduler-properties");
//        初始化factory用以创建scheduler
        Properties schedulerProp = getSchedulerProperties(schedulerConfig);
        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(schedulerProp);
        Scheduler scheduler = factory.getScheduler();
//        添加任务信息
        Element taskConfigs = root.element("task-configs");
//        添加系统类定时任务信息
        Element systaskConfigs = taskConfigs.element("sysTask");
//        默认任务的优先级，系统任务为10，普通任务为5
        int sysPriority = 10;
        int defaultNormalPriority = 5;
        List<Element> systasks = systaskConfigs.elements();
//        系统任务加载
        for (int i = 0, len = systasks.size(); i < len; i++) {
            Element systask = systasks.get(i);
            Element paramConfigE = systask.element("params");
            List<Element> paramConfigs = paramConfigE.elements();
//            读取任务的参数信息
            Properties taskParam = new Properties();
            paramConfigs.forEach(para ->{
                String paraName = para.attributeValue("name");
                String paraVal = para.getTextTrim();
                taskParam.setProperty(paraName, paraVal);
            });
            String taskName = systask.attributeValue("name");
            String type = systask.attributeValue("type");
            String description = systask.elementTextTrim("description");
            String taskClassName = systask.elementTextTrim("task-class");
            String cron = systask.attributeValue("cron", null);
            try {
                Trigger taskTrigger = TriggerCreator.valueOf(type).buildTrigger(cron, "sysTask", taskName,
                        description, sysPriority, taskParam);
                Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(taskClassName);
                JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(taskTrigger.getJobKey()).storeDurably().build();
                scheduler.scheduleJob(jobDetail, taskTrigger);
            } catch (Exception e) {
                logger.error("can not build task {}-{}", "sysTask", taskName, e);
                continue;
            }
        }
//        普通任务加载
        Element normaltaskConfigs = taskConfigs.element("sysTask");
        List<Element> normaltasks = normaltaskConfigs.elements();
//        系统任务加载
        for (int i = 0, len = normaltasks.size(); i < len; i++) {
            Element normalTask = normaltasks.get(i);
            Element paramConfigE = normalTask.element("params");
            List<Element> paramConfigs = paramConfigE.elements();
//            读取任务的参数信息
            Properties taskParam = new Properties();
            paramConfigs.forEach(para ->{
                String paraName = para.attributeValue("name");
                String paraVal = para.getTextTrim();
                taskParam.setProperty(paraName, paraVal);
            });
            String taskName = normalTask.attributeValue("name");
            String type = normalTask.attributeValue("type");
            String description = normalTask.elementTextTrim("description");
            String taskClassName = normalTask.elementTextTrim("task-class");
            String cron = normalTask.attributeValue("cron", null);
            String priority = normalTask.attributeValue("priority", String.valueOf(defaultNormalPriority));
            int priorityVal = Integer.valueOf(priority);
            try {
                Trigger taskTrigger = TriggerCreator.valueOf(type).buildTrigger(cron, "sysTask", taskName,
                        description, priorityVal, taskParam);
                Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(taskClassName);
                JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(taskTrigger.getJobKey()).storeDurably().build();
                scheduler.scheduleJob(jobDetail, taskTrigger);
            } catch (Exception e) {
                logger.error("can not build task {}-{}", "sysTask", taskName, e);
                continue;
            }
        }

        return scheduler;
    }

    /**
     * 生成factory初始化参数
     * @param schedulerConfig
     * @return
     */
    private Properties getSchedulerProperties(Element schedulerConfig) {
        Properties schedulerProp = new Properties();
        List<Element> configs = schedulerConfig.elements();
        for (int i = 0, len = configs.size(); i < len; i++) {
            Element config = configs.get(i);
            String propertyName = config.attributeValue("name");
            String propertyVal = config.getTextTrim();
            schedulerProp.setProperty(propertyName, propertyVal);
        }
        return schedulerProp;
    }

    /**
     * 获取单例
     * @return  调度器单例
     */
    public static ScheduleDeamon getInstance() {
        if (instance == null) {
            synchronized (ScheduleDeamon.class) {
                if (instance == null) {
                    instance = new ScheduleDeamon();
                }
            }
        }
        return instance;
    }


    private static enum TriggerCreator{
        immediate {
            @Override
            public Trigger buildTrigger(String cron, String groupName, String taskName, String description,
                                        int priority, Properties params) throws Exception {
                TriggerKey key = new TriggerKey(taskName, groupName);
                JobDataMap dataMap = buildDataMap(params);
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(key).startNow().withDescription(description).
                        withPriority(priority).withSchedule(SimpleScheduleBuilder.simpleSchedule()).usingJobData(dataMap)
                        .build();
                return trigger;
            }
        },
        cron {
            @Override
            public Trigger buildTrigger(String cron, String groupName, String taskName, String description, int priority, Properties params) throws Exception {
                TriggerKey key = new TriggerKey(taskName, groupName);
                CronExpression.validateExpression(cron);
                JobDataMap dataMap = buildDataMap(params);
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(key).withDescription(description).
                        withPriority(priority).withSchedule(CronScheduleBuilder.cronSchedule(cron)).usingJobData(dataMap)
                        .build();
                return trigger;
            }
        },
        timed {
            @Override
            public Trigger buildTrigger(String cron, String groupName, String taskName, String description,
                                        int priority, Properties params) throws Exception {
                TriggerKey key = new TriggerKey(taskName, groupName);
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startTime = formatter.parse(cron);
                JobDataMap dataMap = buildDataMap(params);
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(key).startAt(startTime).withDescription(description).
                        withPriority(priority).withSchedule(SimpleScheduleBuilder.simpleSchedule()).usingJobData(dataMap)
                        .build();
                return trigger;
            }
        };

        /**
         * 创建触发器
         * @author zhangdatong
         * @param cron  定时任务配置字符串
         * @param groupName 任务组名称
         * @param taskName  任务名称
         * @param params    任务执行时需要的参数
         * @return  Trigger 任务触发器
         * @throws Exception
         */
        public abstract Trigger buildTrigger(String cron, String groupName, String taskName, String description, int priority,
                                    Properties params) throws Exception;

        /**
         * 创建任务运行时的参数列表，统一以字符串的形式存储
         * @author zhangdatong
         * @param params
         * @return JobDataMap
         */
        protected JobDataMap buildDataMap(Properties params) {
            JobDataMap dataMap = new JobDataMap();
            params.forEach((name, val)->{
                dataMap.put((String)name, (String)val);
            });
            return dataMap;
        }
    }

}
