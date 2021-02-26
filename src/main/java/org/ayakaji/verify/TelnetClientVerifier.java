package org.ayakaji.verify;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;

public class TelnetClientVerifier {
	private static final String ip = "10.19.194.134";
	private static final int port = 8023;
	private static final String user = "neteagle";
	private static final String pass = "Huaxun@123";
	private static final TerminalTypeOptionHandler ttoh = new TerminalTypeOptionHandler("VT100", false, false, true,
			false);
	private static final EchoOptionHandler eoh = new EchoOptionHandler(true, false, true, false);
	private static final SuppressGAOptionHandler sgaoh = new SuppressGAOptionHandler(true, true, true, true);

	public static void main(String[] args) throws InvalidTelnetOptionException, IOException {
		TelnetClient tc = new TelnetClient();
		tc.addOptionHandler(ttoh);
		tc.addOptionHandler(eoh);
		tc.addOptionHandler(sgaoh);
		tc.connect(ip, port);
		InputStream is = tc.getInputStream();
		PrintStream os = new PrintStream(tc.getOutputStream());
		readUntil(is, os, "login:");
		write(os, user);
		readUntil(is, os, "assword:");
		write(os, pass);
		readUntil(is, os, "JS-DC01-N7K-1-Access# ");
		write(os, "config");
		readUntil(is, os, "JS-DC01-N7K-1-Access(config)# ");
		write(os, "term len 0");
		readUntil(is, os, "JS-DC01-N7K-1-Access(config)# ");
		write(os, "end");
		readUntil(is, os, "JS-DC01-N7K-1-Access# ");
		write(os, "show cdp neighbors detail | xml");
		System.out.println(readUntil(is, os, "JS-DC01-N7K-1-Access# "));
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
