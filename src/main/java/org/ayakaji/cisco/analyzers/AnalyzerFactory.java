package org.ayakaji.cisco.analyzers;

import io.netty.util.internal.StringUtil;
import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.util.ClassScanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarFile;

/**
 * @author zhangdatong
 * @date 2021/06/08 9:42
 */
public class AnalyzerFactory {

    private static Map<String, Class<? extends ResultAnalyzer>> analyzerMapper;

    static {
        analyzerMapper = new HashMap<String, Class<? extends ResultAnalyzer>>();
        try {
            initAnalyzers();
        } catch (ClassNotFoundException | IOException e) {
        }
    }

    /**
     * 扫描当前包中的类，找到抽象类的实现类并填入Map中
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void initAnalyzers() throws IOException, ClassNotFoundException {
//
        String scanPackage = ResultAnalyzer.class.getPackage().getName();
        String scanPath = scanPackage.replace(".", "/");
        Enumeration<URL> content = Thread.currentThread().getContextClassLoader().getResources(scanPath);
        List<Class<? extends ResultAnalyzer>> analyzers = new ArrayList<Class<? extends ResultAnalyzer>>();
        while (content.hasMoreElements()) {
            URL url = content.nextElement();
            String protocal = url.getProtocol();
            if ("jar".equalsIgnoreCase(protocal)) {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                if (connection != null) {
                    JarFile jarFile = connection.getJarFile();
                    List<Class<? extends ResultAnalyzer>> exeCs = ClassScanner.scanClass(jarFile, scanPackage,
                            ResultAnalyzer.class);
                    analyzers.addAll(exeCs);
                }
            } else {
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                File baseDir = new File(filePath);
                List<Class<? extends ResultAnalyzer>> exeCs = ClassScanner.scanClass(scanPackage, baseDir,
                        ResultAnalyzer.class);
                analyzers.addAll(exeCs);
            }
        }

        for (Iterator<Class<? extends ResultAnalyzer>> iterator = analyzers.iterator(); iterator.hasNext();) {
            Class<? extends ResultAnalyzer> analyzerType = iterator.next();
            AnalyzerName analyzerName = analyzerType.getAnnotation(AnalyzerName.class);
            String label = analyzerName == null || StringUtil.isNullOrEmpty(analyzerName.value()) ?
                    analyzerType.getSimpleName() : analyzerName.value();
            analyzerMapper.put(label, analyzerType);
        }
    }

    public static Class<? extends ResultAnalyzer> getAnalyzerTypeByName (String analyzerName) {
        return analyzerMapper.get(analyzerName);
    }

    public static ResultAnalyzer getAnalyzer(String analyzerName, String hostName, String command, String argument,
                                             Object detectResult, Date detectTime)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends ResultAnalyzer> analyzerType = getAnalyzerTypeByName(analyzerName);
        if (analyzerType == null) {
            analyzerType = getAnalyzerTypeByName("UNKNOWN");
        }
        Constructor<? extends ResultAnalyzer> constructor = analyzerType.getConstructor(String.class, String.class,
                String.class, Object.class, Date.class);
        return constructor.newInstance(hostName, command, argument, detectResult, detectTime);
    }
}
