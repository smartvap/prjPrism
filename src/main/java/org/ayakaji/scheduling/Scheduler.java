/******************************************************
 * The core scheduler for both local and remote agent *
 ******************************************************/
package org.ayakaji.scheduling;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Logger;

import org.ayakaji.conf.IniUtil;
import org.ini4j.Profile.Section;

public class Scheduler {
	private final static Logger logger = Logger.getLogger(Scheduler.class.getName());
	private static Timer timer = null;
	private static final String iniGlobal = "global.ini";
	private static String agentType = "local";
	private static String esAddr = "http://localhost:9200";
	private static long hbDelay = 3000; // heartbeat delay in ms
	private static long hbPeriod = 5000; // heartbeat period in ms
	private static long hbCritical = 600000; // heartbeat critical in ms
	
	private static void parseCfg() {
		Set<Entry<String, Section>> cfg = IniUtil.readIni(iniGlobal);
		if (cfg == null || cfg.isEmpty()) {
			logger.warning("Global configurations are missing.");
			System.exit(0);
		}
		Iterator<Entry<String, Section>> itr = cfg.iterator();
		while (itr.hasNext()) {
			Entry<String, Section> ent = itr.next();
			if (ent.getKey().equals("agent")) {
				if (ent.getValue().get("type").equals("remote")) {
					agentType = "remote";
				}
			} else if (ent.getKey().equals("elsticsearch")) {
				String addr = ent.getValue().get("addr");
				if (addr != null && !addr.equals("")) {
					esAddr = addr;
				}
			} else if (ent.getKey().equals("heartbeat")) {
				String delay = ent.getValue().get("delay");
				String period = ent.getValue().get("period");
				String critical = ent.getValue().get("critical");
				if (delay != null && !delay.equals("")) {
					try {
						hbDelay = Long.parseLong(delay);
					} catch (NumberFormatException e) {
						logger.warning("Illegal Configurations.");
					}
				}
				if (period != null && !period.equals("")) {
					try {
						hbPeriod = Long.parseLong(period);
					} catch (NumberFormatException e) {
						logger.warning("Illegal Configurations.");
					}
				}
				if (critical != null && !critical.equals("")) {
					try {
						hbCritical = Long.parseLong(critical);
					} catch (NumberFormatException e) {
						logger.warning("Illegal Configurations.");
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		parseCfg();
	}
	
	private static void heartbeat() {
		
	}

}
