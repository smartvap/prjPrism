package org.ayakaji.network.prism;

import org.ayakaji.conf.IniUtil;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class TestIni {

    @Test
    public void testReadIniItems() throws IOException {

        String iniName = "global.ini";
        File iniFile = new File(iniName);
        Ini iniReader = new Ini(iniFile);
        Map<String, String> configs = iniReader.get("JS-DC01-N7K-1-Access");
        configs.forEach((key, val) -> {
            System.out.println(String.join("\t", key, val));
        });
    }

    @Test
    public void testReadAllIniItems() throws Exception {
        String iniName = "target\\testIni.ini";
        File iniFile = new File(iniName);
        Ini iniReader = new Ini(iniFile);
        iniReader.forEach((name, section)->{
            System.out.println(name);
            System.out.println(section);
        });

    }


}
