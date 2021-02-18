package org.ayakaji.network.prism;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class IfConfig {
	private static Logger LOGGER = Logger.getLogger(IfConfig.class.getName());

	private static final boolean PREFER_V6 = Boolean
			.parseBoolean(System.getProperty("java.net.preferIPv6Addresses", "false"));
	private static final boolean SUPPORTS_V6;
	static {
		boolean v = false;
		try {
			for (NetworkInterface nic : getInterfaces()) {
				for (InetAddress address : Collections.list(nic.getInetAddresses())) {
					if (address instanceof Inet6Address) {
						v = true;
						break;
					}
				}
			}
		} catch (SecurityException | SocketException misconfiguration) {
			v = true;
		}
		SUPPORTS_V6 = v;
	}

	/**
	 * Sorts an address by preference. This way code like publishing can just pick
	 * the first one
	 */
	static int sortKey(InetAddress address, boolean prefer_v6) {
		int key = address.getAddress().length;
		if (prefer_v6) {
			key = -key;
		}

		if (address.isAnyLocalAddress()) {
			key += 5;
		}
		if (address.isMulticastAddress()) {
			key += 4;
		}
		if (address.isLoopbackAddress()) {
			key += 3;
		}
		if (address.isLinkLocalAddress()) {
			key += 2;
		}
		if (address.isSiteLocalAddress()) {
			key += 1;
		}

		return key;
	}

	/**
	 * Sorts addresses by order of preference. This is used to pick the first one
	 * for publishing.
	 */
	private static void sortAddresses(List<InetAddress> list) {
		Collections.sort(list, new Comparator<InetAddress>() {
			@Override
			public int compare(InetAddress left, InetAddress right) {
				int cmp = Integer.compare(sortKey(left, PREFER_V6), sortKey(right, PREFER_V6));
				if (cmp == 0) {
//					cmp = new BytesRef(left.getAddress()).compareTo(new BytesRef(right.getAddress()));
				}
				return cmp;
			}
		});
	}

	/**
	 * Return all interfaces (and sub-interfaces such as network aliases)
	 * 
	 * @return
	 * @throws SocketException
	 */
	private static List<NetworkInterface> getInterfaces() throws SocketException {
		List<NetworkInterface> all = new ArrayList<NetworkInterface>();
		addAllInterfaces(all, Collections.list(NetworkInterface.getNetworkInterfaces()));
		Collections.sort(all, new Comparator<NetworkInterface>() {
			@Override
			public int compare(NetworkInterface left, NetworkInterface right) {
				return Integer.compare(left.getIndex(), right.getIndex());
			}
		});
		return all;
	}

	/**
	 * Get all interfaces and sub-interfaces
	 * @param target
	 * @param level
	 */
	private static void addAllInterfaces(List<NetworkInterface> target, List<NetworkInterface> level) {
		if (!level.isEmpty()) {
			target.addAll(level);
			for (NetworkInterface intf : level) {
				addAllInterfaces(target, Collections.list(intf.getSubInterfaces()));
			}
		}
	}

//	public static boolean defaultReuseAddress() {
//		return Constants.WINDOWS ? false : true;
//	}

	private static InetAddress[] getLoopbackAddresses() throws SocketException {
		List<InetAddress> list = new ArrayList<>();
		for (NetworkInterface intf : getInterfaces()) {
			if (intf.isLoopback() && intf.isUp()) {
				list.addAll(Collections.list(intf.getInetAddresses()));
			}
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException("No up-and-running loopback interfaces found, got " + getInterfaces());
		}
		sortAddresses(list);
		return list.toArray(new InetAddress[list.size()]);
	}

	static InetAddress[] getFirstNonLoopbackAddresses() throws SocketException {
		List<InetAddress> list = new ArrayList<>();
		for (NetworkInterface intf : getInterfaces()) {
			if (intf.isLoopback() == false && intf.isUp()) {
				list.addAll(Collections.list(intf.getInetAddresses()));
				break;
			}
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException(
					"No up-and-running non-loopback interfaces found, got " + getInterfaces());
		}
		sortAddresses(list);
		return list.toArray(new InetAddress[list.size()]);
	}

	static InetAddress[] getAddressesForInterface(String name) throws SocketException {
		NetworkInterface intf = NetworkInterface.getByName(name);
		if (intf == null) {
			throw new IllegalArgumentException("No interface named '" + name + "' found, got " + getInterfaces());
		}
		if (!intf.isUp()) {
			throw new IllegalArgumentException("Interface '" + name + "' is not up and running");
		}
		List<InetAddress> list = Collections.list(intf.getInetAddresses());
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Interface '" + name + "' has no internet addresses");
		}
		sortAddresses(list);
		return list.toArray(new InetAddress[list.size()]);
	}

	static InetAddress[] getAllByName(String host) throws UnknownHostException {
		InetAddress addresses[] = InetAddress.getAllByName(host);
		List<InetAddress> unique = new ArrayList<>(new HashSet<>(Arrays.asList(addresses)));
		sortAddresses(unique);
		return unique.toArray(new InetAddress[unique.size()]);
	}

	static InetAddress[] filterIPV4(InetAddress addresses[]) {
		List<InetAddress> list = new ArrayList<>();
		for (InetAddress address : addresses) {
			if (address instanceof Inet4Address) {
				list.add(address);
			}
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException("No ipv4 addresses found in " + Arrays.toString(addresses));
		}
		return list.toArray(new InetAddress[list.size()]);
	}

	static InetAddress[] filterIPV6(InetAddress addresses[]) {
		List<InetAddress> list = new ArrayList<>();
		for (InetAddress address : addresses) {
			if (address instanceof Inet6Address) {
				list.add(address);
			}
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException("No ipv6 addresses found in " + Arrays.toString(addresses));
		}
		return list.toArray(new InetAddress[list.size()]);
	}
	
	private static String getInterfacesJSON() throws SocketException {
		List<NetworkInterface> all = getInterfaces();
		return JSON.toJSONString(all, true);
	}

	public static void main(String[] args) throws SocketException {
		LOGGER.info(getInterfacesJSON());
	}

}
