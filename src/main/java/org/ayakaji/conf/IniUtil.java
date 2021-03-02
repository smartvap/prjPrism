package org.ayakaji.conf;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Map.Entry;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class IniUtil {
	private static final Logger logger = Logger.getLogger(IniUtil.class.getName());
	
	public static Set<Entry<String, Section>> readIni(String iniPath) {
		Ini ini = new Ini();
		File fIni = new File(iniPath);
		if (!fIni.exists() || !fIni.isFile() || !fIni.canRead()) {
			logger.warning("Config file " + iniPath + " does not exists or can not be read.");
			return null;
		}
		try {
			ini.load(fIni);
		} catch (IOException e) {
			logger.warning("An exception occurred while reading the configuration file, "
					+ "which may be caused by an incorrect file format.");
			return null;
		}
		return ini.entrySet();
	}

	public static void main(String[] args) {
		

	}

}
