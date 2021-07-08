/**
 * Apache Common Net encapsulation
 * Author: Hugh
 */
package org.ayakaji.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.Map.Entry;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.log4j.Logger;
import org.ini4j.Profile.Section;

public class CommonNetWrapper {
	private static final Logger logger = Logger.getLogger(CommonNetWrapper.class.getName());
	private static final TerminalTypeOptionHandler ttoh = new TerminalTypeOptionHandler("VT100", false, false, true,
			false);
	private static final EchoOptionHandler eoh = new EchoOptionHandler(true, false, true, false);
	private static final SuppressGAOptionHandler sgaoh = new SuppressGAOptionHandler(true, true, true, true);

	private TelnetClient tc = null;
	private InputStream is = null;
	private PrintStream ps = null;

	private String ip; // Device Mgmt IP
	private int port; // Device Mgmt Port
	private String acct; // Device Mgmt Account
	private String pass; // Device Mgmt Password
	private String id; // Device ID

	public CommonNetWrapper(Entry<String, Section> host) {
		id = host.getKey();
		ip = host.getValue().get("MGR_IP");
		port = Integer.parseInt(host.getValue().get("MGR_PORT"));
		acct = host.getValue().get("ACCOUNT");
		pass = host.getValue().get("PASSWORD");
	}

	/**
	 * 
	 * @return
	 */
	public boolean connect() {
		tc = new TelnetClient();
		tc.setConnectTimeout(3000);
		try {
			tc.setSoTimeout(3000);
		} catch (SocketException e) {
			logger.warn("Cannot set socket timeout.");
			return false;
		}
		try {
			tc.addOptionHandler(ttoh);
			tc.addOptionHandler(eoh);
			tc.addOptionHandler(sgaoh);
		} catch (InvalidTelnetOptionException | IOException e) {
			logger.warn("Cannot set telnet option.");
			return false;
		}
		try {
			tc.connect(ip, port);
		} catch (IOException e) {
			logger.warn("Cannot establish connection with " + ip + ":" + port);
			return false;
		}
		return true;
	}

	/**
	 * Disconnect from telnet server.
	 * 
	 */
	public void disconnect() {
		try {
			is.close();
		} catch (IOException e) {
			logger.warn("An exception occurs when closing input stream.");
		}
		ps.close();
		try {
			tc.disconnect();
		} catch (IOException e) {
			logger.warn("An exception occurs when disconnecting telnet.");
		}
		tc = null;
	}

	/**
	 * Cisco Nexus 7xx Series Login Method
	 * 
	 * @return
	 */
	public boolean login_n7k() {
		is = tc.getInputStream();
		ps = new PrintStream(tc.getOutputStream());
		try {
			readUntil("login:");
		} catch (IOException e) {
			logger.warn("Failed to wait for the login prompt.");
			disconnect();
			return false;
		}
		write(acct);
		try {
			readUntil("assword:");
		} catch (IOException e) {
			logger.warn("Failed to wait for the password prompt.");
			disconnect();
			return false;
		}
		write(pass);
		try {
			readUntil(id + "# ");
		} catch (IOException e) {
			logger.warn("Failed to wait for the " + id + "# prompt.");
			disconnect();
			return false;
		}
		write("config");
		try {
			readUntil(id + "(config)# ");
		} catch (IOException e) {
			logger.warn("Cannot enter configuration mode.");
			disconnect();
			return false;
		}
		write("term len 0");
		try {
			readUntil(id + "(config)# ");
		} catch (IOException e) {
			logger.warn("An exception occurs when disabling split screen mode.");
			disconnect();
			return false;
		}
		write("end");
		try {
			readUntil(id + "# ");
		} catch (IOException e) {
			logger.warn("An exception occurs when ending configurations.");
			disconnect();
			return false;
		}
		return true;
	}

	/**
	 * Cisco Discovery Protocol (CDP) Neighbor Information
	 */
	public void showCdpNeighbors() {
		write("show cdp neighbors detail | xml");
		String xml;
		try {
			xml = readUntil(id + "# ");
		} catch (IOException e) {
			e.printStackTrace();
		}
//		xml = xml.replaceFirst("show cdp neighbors detail \\| xml\r\r", "");
//		xml = xml.replaceFirst("\n", "");
//		xml = xml.replaceFirst("\\]\\]\\>\\]\\]\\>\r", "");
//		xml = xml.replaceFirst("JS-DC01-N7K-1-Access# ", "");
//		StringReader sr = new StringReader(xml);
//		InputSource is2 = new InputSource(sr);
//		Document doc = new SAXReader().read(is2);
//		List<?> list = doc.selectNodes("//ROW_cdp_neighbor_detail_info");
//		System.out.println(list.size());
	}

	/**
	 * Read input stream until encountered prompt. <Bug> In split-screen mode, the
	 * split-screen prompt "--More--" will cause garbled characters. After deleting
	 * this prompt, the effect is not good. Disable the split screen mode through
	 * configure term len 0, but it will cause a potential command stuck risk, which
	 * will cause a backlog of the telnet process. </Bug>
	 * 
	 * @param pattern
	 * @return
	 * @throws IOException
	 */
	private String readUntil(String pattern) throws IOException {
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
					write(' ');
				}
			}
			ch = (char) is.read();
		}
	}

	/**
	 * Write char to PrintStream
	 * 
	 * @param value
	 */
	private void write(char value) {
		if (ps != null) {
			ps.print(value);
			ps.flush();
		}
	}

	private void write(String value) {
		if (ps != null) {
			ps.println(value);
			ps.flush();
		}
	}

}
