package org.ayakaji.verify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DomVerifier {

	private static Logger logger = Logger.getLogger(DomVerifier.class.getName());

	public static void main(String[] args) throws DocumentException {
		String xml = "abcd\r\n<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><nf:rpc-reply xmlns:nf=\"urn:ietf:params:xml:ns:netconf:base:1.0\" xmlns=\"http://www.cisco.com/nxos:1.0:cdpd\">\r\n"
				+ " <nf:data>\r\n" + "  <show>\r\n" + "   <cdp>\r\n" + "    <neighbors>\r\n"
				+ "     <__XML__OPT_Cmd_show_cdp_neighbors_interface>\r\n"
				+ "      <__XML__OPT_Cmd_show_cdp_neighbors_detail___readonly__>\r\n" + "       <__readonly__>\r\n"
				+ "        <TABLE_cdp_neighbor_detail_info>\r\n" + "         <ROW_cdp_neighbor_detail_info>\r\n"
				+ "          <ifindex a=\"1\">436207616</ifindex>\r\n"
				+ "          <device_id>GQT-DC02-N7K-1-Access(JAF1638AABD)</device_id>\r\n"
				+ "          <sysname>GQT-DC02-N7K-1-Access</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.203</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet1/1</intf_id>\r\n" + "          <port_id>Ethernet1/1</port_id>\r\n"
				+ "          <ttl>121</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>98</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>0</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>436301824</ifindex>\r\n"
				+ "          <device_id>JS-DC01-C4507R-JingFen</device_id>\r\n"
				+ "          <vtpname>sdyd</vtpname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.29</v4addr>\r\n"
				+ "          <platform_id>cisco WS-C4507R-E</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <intf_id>Ethernet1/24</intf_id>\r\n"
				+ "          <port_id>GigabitEthernet1/23</port_id>\r\n" + "          <ttl>165</ttl>\r\n"
				+ "          <version>Cisco IOS Software, Catalyst 4500 L3 Switch Software (cat4500-ENTSERVICES-M), Version 12.2(31)SGA11, RELEASE SOFTWARE (fc1)\r\n"
				+ "Technical Support: http://www.cisco.com/techsupport\r\n"
				+ "Copyright (c) 1986-2010 by Cisco Systems, Inc.\r\n"
				+ "Compiled Fri 08-Jan-10 17:07 by alnguyen</version>\r\n" + "          <version_no>v2</version_no>\r\n"
				+ "          <nativevlan>1</nativevlan>\r\n" + "          <duplexmode>full</duplexmode>\r\n"
				+ "          <mtu>0</mtu>\r\n" + "          <num_mgmtaddr>1</num_mgmtaddr>\r\n"
				+ "          <v4mgmtaddr>10.19.93.29</v4mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>436318208</ifindex>\r\n"
				+ "          <device_id>JS-DC01-C6509-1-Kernel.sdboss.com</device_id>\r\n"
				+ "          <vtpname>null</vtpname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.212</v4addr>\r\n"
				+ "          <platform_id>cisco WS-C6509-E</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <intf_id>Ethernet1/28</intf_id>\r\n"
				+ "          <port_id>TenGigabitEthernet7/2</port_id>\r\n" + "          <ttl>129</ttl>\r\n"
				+ "          <version>Cisco Internetwork Operating System Software \r\n"
				+ "IOS (tm) s72033_rp Software (s72033_rp-IPSERVICESK9_WAN-M), Version 12.2(18)SXF11, RELEASE SOFTWARE (fc1)\r\n"
				+ "Technical Support: http://www.cisco.com/techsupport\r\n"
				+ "Copyright (c) 1986-2007 by cisco Systems, Inc.\r\n"
				+ "Compiled Fri 14-Sep-07 21:50 by kellythw</version>\r\n" + "          <version_no>v2</version_no>\r\n"
				+ "          <nativevlan>1</nativevlan>\r\n" + "          <duplexmode>full</duplexmode>\r\n"
				+ "          <mtu>0</mtu>\r\n" + "          <num_mgmtaddr>0</num_mgmtaddr>\r\n"
				+ "         </ROW_cdp_neighbor_detail_info>\r\n" + "         <ROW_cdp_neighbor_detail_info>\r\n"
				+ "          <ifindex>436363264</ifindex>\r\n"
				+ "          <device_id>JS-DC01-N7K-2-Access(JAF1811AGEE)</device_id>\r\n"
				+ "          <sysname>JS-DC01-N7K-2-Access</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.209</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet1/39</intf_id>\r\n" + "          <port_id>Ethernet1/39</port_id>\r\n"
				+ "          <ttl>125</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>1</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>0</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>436367360</ifindex>\r\n"
				+ "          <device_id>JS-DC01-N7K-2-Access(JAF1811AGEE)</device_id>\r\n"
				+ "          <sysname>JS-DC01-N7K-2-Access</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.209</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet1/40</intf_id>\r\n" + "          <port_id>Ethernet1/40</port_id>\r\n"
				+ "          <ttl>125</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>1</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>0</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>439353344</ifindex>\r\n"
				+ "          <device_id>KFQ-DC03-N7K-1(FXS1933Q3HT)</device_id>\r\n"
				+ "          <sysname>KFQ-DC03-N7K-1</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.155</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet7/1</intf_id>\r\n" + "          <port_id>Ethernet1/1</port_id>\r\n"
				+ "          <ttl>121</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>94</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>1500</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>439447552</ifindex>\r\n"
				+ "          <device_id>JS-DC01-C4507R-JingFen</device_id>\r\n"
				+ "          <vtpname>sdyd</vtpname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.29</v4addr>\r\n"
				+ "          <platform_id>cisco WS-C4507R-E</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <intf_id>Ethernet7/24</intf_id>\r\n"
				+ "          <port_id>GigabitEthernet2/26</port_id>\r\n" + "          <ttl>123</ttl>\r\n"
				+ "          <version>Cisco IOS Software, Catalyst 4500 L3 Switch Software (cat4500-ENTSERVICES-M), Version 12.2(31)SGA11, RELEASE SOFTWARE (fc1)\r\n"
				+ "Technical Support: http://www.cisco.com/techsupport\r\n"
				+ "Copyright (c) 1986-2010 by Cisco Systems, Inc.\r\n"
				+ "Compiled Fri 08-Jan-10 17:07 by alnguyen</version>\r\n" + "          <version_no>v2</version_no>\r\n"
				+ "          <nativevlan>1</nativevlan>\r\n" + "          <duplexmode>full</duplexmode>\r\n"
				+ "          <mtu>0</mtu>\r\n" + "          <num_mgmtaddr>1</num_mgmtaddr>\r\n"
				+ "          <v4mgmtaddr>10.19.93.29</v4mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>439508992</ifindex>\r\n"
				+ "          <device_id>JS-DC01-N7K-2-Access(JAF1811AGEE)</device_id>\r\n"
				+ "          <sysname>JS-DC01-N7K-2-Access</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.209</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet7/39</intf_id>\r\n" + "          <port_id>Ethernet7/39</port_id>\r\n"
				+ "          <ttl>125</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>1</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>0</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "         <ROW_cdp_neighbor_detail_info>\r\n" + "          <ifindex>439513088</ifindex>\r\n"
				+ "          <device_id>JS-DC01-N7K-2-Access(JAF1811AGEE)</device_id>\r\n"
				+ "          <sysname>JS-DC01-N7K-2-Access</sysname>\r\n" + "          <numaddr>1</numaddr>\r\n"
				+ "          <v4addr>10.19.93.209</v4addr>\r\n" + "          <platform_id>N7K-C7010</platform_id>\r\n"
				+ "          <capability>router</capability>\r\n" + "          <capability>switch</capability>\r\n"
				+ "          <capability>IGMP_cnd_filtering</capability>\r\n"
				+ "          <capability>Supports-STP-Dispute</capability>\r\n"
				+ "          <intf_id>Ethernet7/40</intf_id>\r\n" + "          <port_id>Ethernet7/40</port_id>\r\n"
				+ "          <ttl>125</ttl>\r\n"
				+ "          <version>Cisco Nexus Operating System (NX-OS) Software, Version 6.2(16)</version>\r\n"
				+ "          <version_no>v2</version_no>\r\n" + "          <nativevlan>1</nativevlan>\r\n"
				+ "          <duplexmode>full</duplexmode>\r\n" + "          <mtu>0</mtu>\r\n"
				+ "          <num_mgmtaddr>0</num_mgmtaddr>\r\n" + "         </ROW_cdp_neighbor_detail_info>\r\n"
				+ "        </TABLE_cdp_neighbor_detail_info>\r\n" + "       </__readonly__>\r\n"
				+ "      </__XML__OPT_Cmd_show_cdp_neighbors_detail___readonly__>\r\n"
				+ "     </__XML__OPT_Cmd_show_cdp_neighbors_interface>\r\n" + "    </neighbors>\r\n" + "   </cdp>\r\n"
				+ "  </show>\r\n" + " </nf:data>\r\n" + "</nf:rpc-reply>\r\n" + "\r\n" + "";
		String regex = ".*(\\<\\?xml [.|\\s|\\S]*\\<\\/nf\\:rpc-reply\\>)";
		Pattern pattern = Pattern.compile(regex);
		Matcher m = pattern.matcher(xml);
		if (m.find()) {
			xml = m.group(1);
		}

		Document doc = DocumentHelper.parseText(xml);
		Element root = doc.getRootElement();
		Element table = search(root, "TABLE_cdp_neighbor_detail_info");
		List<Element> rows = search(root, "ROW_cdp_neighbor_detail_info", null);
		JSONArray arrCDP = new JSONArray();
		JSONObject jsonCDP = new JSONObject(new LinkedHashMap<String, Object>());
		for (Element row : rows) {
			Iterator<?> itr = row.nodeIterator();
			while (itr.hasNext()) {
				Object o = itr.next();
				if (o instanceof DefaultElement) {
					DefaultElement e = (DefaultElement) o;
					System.out.println(e.node(0).getNodeTypeName());
					System.exit(0);
				}
			}
			System.out.println();
		}
	}

	/**
	 * Recursive search for the first matching element
	 * 
	 * @param parent
	 * @param nodeName
	 * @return
	 */
	private static Element search(Element parent, String nodeName) {
		if (parent == null || nodeName == null || nodeName.equals("")) {
			logger.warn("Part or all of the parameters are illegal.");
			return null;
		}
		if (parent.getName().equals(nodeName))
			return parent;
		Iterator<?> itrSubNode = parent.nodeIterator();
		while (itrSubNode.hasNext()) {
			Object obj = itrSubNode.next();
			if (obj instanceof DefaultElement) {
				DefaultElement de = (DefaultElement) obj;
				if (de.getName().equals(nodeName))
					return de;
				else
					return search(de, nodeName);
			}
		}
		return null;
	}

	/**
	 * Recursive search for all matching elements
	 * 
	 * @param parent
	 * @param nodeName
	 * @param collector
	 * @return
	 */
	private static List<Element> search(Element parent, String nodeName, List<Element> collector) {
		if (parent == null || nodeName == null || nodeName.equals("")) {
			logger.warn("Part or all of the parameters are illegal.");
			return null;
		}
		if (collector == null) // Initialize collector
			collector = new ArrayList<Element>();
		if (parent.getName().equals(nodeName))
			collector.add(parent);
		Iterator<?> itrSubNode = parent.nodeIterator();
		while (itrSubNode.hasNext()) {
			Object obj = itrSubNode.next();
			if (obj instanceof DefaultElement) {
				DefaultElement de = (DefaultElement) obj;
				if (de.getName().equals(nodeName))
					collector.add(de);
				else
					return search(de, nodeName, collector);
			}
		}
		return collector;
	}

	/**
	 * Node Iterator to JSON
	 * 
	 * @param itr
	 * @return
	 */
//	private static JSONObject dom2json(Iterator<Element> itr) {
//		if (itr == null)
//			return null;
//		JSONObject j = new JSONObject(new LinkedHashMap<String, Object>());
//		Iterator<?> itr = e.nodeIterator();
//		while (itr.hasNext()) {
//			JSONObject k = new JSONObject(new LinkedHashMap<String, Object>());
//			Object o = itr.next();
//			if (o instanceof DefaultText) { // Current node is leaf node
//				DefaultText t = (DefaultText) o;
//				k.put(e.getName(), t.getText());
//				return k;
//			} else if (o instanceof DefaultElement) {
//				DefaultElement f = (DefaultElement) o;
//				JSONObject l = dom2json(f);
//				l.keySet().toArray()[0]
//			}
//		}
//		return null;
//	}

	/**
	 * DOM Array to JSON Array.
	 * 
	 * @param l
	 * @return
	 */
	private static JSONArray dom2json(List<Element> l) {
//		List<Element> rows = search(root, "ROW_cdp_neighbor_detail_info", null);
//		JSONArray arrCDP = new JSONArray();
//		JSONObject jsonCDP = new JSONObject(new LinkedHashMap<String, Object>());
//		for (Element row : rows) {
//			Iterator<?> itr = row.nodeIterator();
//			while (itr.hasNext()) {
//				Object elem = itr.next();
//				if (elem instanceof DefaultElement)
//					System.out.print(((DefaultElement) elem).getName() + ", ");
//			}
//			System.out.println();
//		}
		return null;
	}

}
