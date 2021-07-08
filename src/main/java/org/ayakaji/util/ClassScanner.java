package org.ayakaji.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 扫描接口的实现类
 *
 * @author zhangdatong
 * @version 1.0.0
 * @date 2021/06/08 13:39
 */
public class ClassScanner {

    /**
     * @param basePath
     * @param dir
     * @param classOfT
     * @return
     * @throws ClassNotFoundException
     */
    public static <T> List<Class<? extends T>> scanClass(String basePath, File dir, Class<T> classOfT)
            throws ClassNotFoundException {

        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        File[] files = dir.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".class") && !fileName.contains("$")) {
                String className = basePath.concat(".").concat(fileName.substring(0, fileName.lastIndexOf(".class")));
                Class<?> cl = Class.forName(className);

                if (classOfT.isAssignableFrom(cl) && classOfT != cl) {
                    Class<? extends T> cll = cl.asSubclass(classOfT);
                    result.add(cll);
                }

            } else if (file.isDirectory()) {
                List<Class<? extends T>> subs = scanClass(basePath.concat(".").concat(file.getName()), file, classOfT);
                result.addAll(subs);
            }
        }
        return result;
    }

    public static <T> List<Class<? extends T>> scanClass(JarFile jarFile, String basePack, Class<T> classOfT)
            throws ClassNotFoundException {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry entry = jarEntryEnumeration.nextElement();
            String jarEntryName = entry.getName();
            if (jarEntryName.endsWith(".class") && jarEntryName.replaceAll("/", ".")
                    .startsWith(basePack) && !jarEntryName.contains("$")) {
                String className = jarEntryName.substring(0, jarEntryName.lastIndexOf("."))
                        .replace("/", ".");
                Class<?> cl = Class.forName(className);
                if (classOfT.isAssignableFrom(cl) && classOfT != cl) {
                    Class<? extends T> cll = cl.asSubclass(classOfT);
                    result.add(cll);
                }
            }
        }

        return result;

    }

}
