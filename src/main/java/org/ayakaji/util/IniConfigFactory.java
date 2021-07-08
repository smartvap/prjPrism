package org.ayakaji.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载Ini配置文件信息到公共配置工具类
 * @author zhangdatong
 * @version 1.0.0
 *  date 2021.04.26
 */
public class IniConfigFactory {

    private static transient Logger logger = LogManager.getLogger(IniConfigFactory.class);

    /**
     * 获取配置文件路径，调用方法加载配置信息
     * @since 1.0.0
     * @author zhangdatong
     * date 2021.04.26
     */
    static {
//        获取配置文件信息，优先获取jar包外的配置文件信息，如果没有，则改为获取jar包中的默认配置文件
        Path iniFilePath = Paths.get("global.ini");
        if (!Files.exists(iniFilePath)) {
            URL defaultIniFile = IniConfigFactory.class.getClassLoader().getResource("global.ini");
            iniFilePath = Paths.get(defaultIniFile.getPath());
        }
//        调用方法加载配置信息
        init(iniFilePath);
    }

    private static Map<String, Profile.Section> hostConfig;     //主机配置信息
    private static Map<String, String> commonCmd;       //公共命令配置信息
    private static Map<String, String> commonConfig;    //公共配置信息

    /**
     * 加载配置文件信息
     * @param configPath    配置文件路径
     */
    public static void init(Path configPath) {
        hostConfig = new HashMap<String, Profile.Section>();
        commonCmd = new HashMap<String, String>();
        commonConfig = new HashMap<String, String>();
        try {
            Ini iniReader = new Ini(configPath.toFile());
            iniReader.forEach((name, section)-> {
                ConfigLoader loader = null;
                try {
                    loader = ConfigLoader.valueOf(name);
                } catch (IllegalArgumentException e) {
                    loader = ConfigLoader.DEFAULT_LOADER;
                }
                if (loader == null) {
                    loader = ConfigLoader.DEFAULT_LOADER;
                }
                loader.loadConfig(name, section);
            });
        } catch (IOException e) {
//            如果加载过程中出现问题，则大多数的任务都无法进行，因此退出系统
            logger.error("can not read ini config with file: {}", configPath.toAbsolutePath().toString(), e);
            System.exit(1);
        }
    }

    public static String getCommonConfig(String configName) {
        return commonConfig.get(configName);
    }

    public static String getCommonCmd(String cmdName) {
        return commonCmd.get(cmdName);
    }

    public static Profile.Section getHostConfigSection(String hostName) {
        return hostConfig.get(hostName);
    }

    public static String getHostConfig(String hostName, String configName) {
        Profile.Section configContent = hostConfig.get(hostName);
        return configContent == null ? null : configContent.get(configName);
    }

    static enum ConfigLoader{
        COMMON_CMD {
            @Override
            public void loadConfig(String sectionCatagory, Profile.Section content) {
                content.forEach((key, val)-> {
                    commonCmd.put(key, val);
                });
            }
        },
        COMMON_CONFIG {
            @Override
            public void loadConfig(String sectionCatagory, Profile.Section content) {
                content.forEach((key, val) -> {
                    commonConfig.put(key, val);
                });
            }
        },
        DEFAULT_LOADER;

        public void loadConfig (String sectionCatagory, Profile.Section content) {
            hostConfig.put(sectionCatagory, content);
        }
    }
}
