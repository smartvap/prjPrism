package org.ayakaji.cisco;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.net.telnet.TelnetClient;
import org.ayakaji.conf.IniUtil;
import org.ini4j.Profile.Section;

public class CiscoWrapper {
	private static final Logger logger = Logger.getLogger(CiscoWrapper.class.getName());
	private static final String CISCO_INI = "cisco.ini";
	private static final Set<Entry<String, Section>> ciscoSystems;

	static {
		ciscoSystems = IniUtil.readIni(CISCO_INI);
		// Add Config Verify
	}

	public static void main(String[] args) throws SocketException, IOException {
		// Add Connect
		// Call Command
		// Regex Parse
		// DOM Read
		
		TelnetClient tc = new TelnetClient();
//		tc.connect(ip, port);
		InputStream is = tc.getInputStream();
		PrintStream os = new PrintStream(tc.getOutputStream());
		readUntil(is, os, "login:");
//		write(os, user);
		readUntil(is, os, "assword:");
//		write(os, pass);
		System.out.println(readUntil(is, os, "# "));
		tc.disconnect();
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
