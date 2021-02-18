package org.ayakaji.network.prism;

import java.util.List;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import com.alibaba.fastjson.JSON;

@SuppressWarnings("unused")
public class Pcap4jWrapper {

	public static void main(String[] args) throws PcapNativeException, NotOpenException, InterruptedException {
		capture("134.80.19.88");
	}

	private static void dumpAllNetworkIntfaces() throws PcapNativeException {
		List<PcapNetworkInterface> allIntfs = Pcaps.findAllDevs();
		System.out.println(JSON.toJSONString(allIntfs, true));
	}

	private static PcapNetworkInterface getNetworkInterface(String host) throws PcapNativeException {
		List<PcapNetworkInterface> allIntfs = Pcaps.findAllDevs();
		for (PcapNetworkInterface intf : allIntfs) {
			List<PcapAddress> addrs = intf.getAddresses();
			for (PcapAddress addr : addrs) {
				String ip = addr.getAddress().getHostAddress();
				if (ip != null && ip.equals(host)) {
					return intf;
				}
			}
		}
		return null;
	}

	private static void capture(String host) throws PcapNativeException, NotOpenException, InterruptedException {
		PcapNetworkInterface nif = getNetworkInterface(host);
		if (nif == null)
			return;
		PcapHandle pHnd = nif.openLive(64 * 1024, PromiscuousMode.PROMISCUOUS, 50);
//		pHnd.setFilter("(tcp or udp) and (dst host 10.17.253.11 and dst port 53)", BpfCompileMode.OPTIMIZE);
//		PcapDumper dumper = pHnd.dumpOpen("dump.pcap");
		pHnd.loop(1000, new CoreCaptureListener());
		System.out.print("Captured 1000 Packets");
	}

	private static class CoreCaptureListener implements PacketListener {

		@Override
		public void gotPacket(Packet pkt) {
			byte[] bytes = pkt.getRawData();
			// Do nothing
		}

	}

}
