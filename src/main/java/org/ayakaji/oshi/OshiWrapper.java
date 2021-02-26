package org.ayakaji.oshi;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.Firmware;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.software.os.OperatingSystemVersion;
import oshi.software.os.windows.WindowsOperatingSystem;
import oshi.util.EdidUtil;

public class OshiWrapper {

	private final static Logger logger = Logger.getLogger(OshiWrapper.class.getName());

	private final static Set<String> invalidValues = new HashSet<String>() {
		private static final long serialVersionUID = -2991574982829423335L;
		{
			add("unknown");
			add("");
			add("null");
		}
	};

	private static boolean isValid(String val) {
		if (invalidValues.contains(val.toLowerCase()))
			return false;
		return true;
	}

	private static JSONObject getSystemInfo() {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		SystemInfo si = new SystemInfo();
		json.put("hardware", getHardware(si));
		json.put("operatingSystem", getOperatingSystem(si));
		return json;
	}

	private static JSONObject getHardware(SystemInfo si) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		HardwareAbstractionLayer hal = si.getHardware();
		json.put("computerSystem", getComputerSystem(hal));
		json.put("diskStores", getDiskStores(hal));
		JSONArray displays = getDisplays(hal);
		if (displays.size() != 0)
			json.put("displays", displays);
		json.put("memory", getMemory(hal));
		json.put("networkIFs", getNetworkIFs(hal));
		JSONArray pss = getPowerSources(hal);
		if (pss.size() != 0)
			json.put("powerSources", pss);
		json.put("processor", getProcessor(hal));
		JSONObject sensor = getSensors(hal);
		if (!sensor.isEmpty())
			json.put("sensors", sensor);
		JSONArray scs = getSoundCards(hal);
		if (scs.size() != 0)
			json.put("soundCards", scs);
		return json;
	}

	private static JSONObject getComputerSystem(HardwareAbstractionLayer hal) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		ComputerSystem cs = hal.getComputerSystem();
		JSONObject jsonBb = getBaseboard(cs);
		if (!jsonBb.isEmpty())
			json.put("baseboard", jsonBb);
		JSONObject jsonFw = getFirmware(cs);
		if (!jsonFw.isEmpty())
			json.put("firmware", jsonFw);
		if (isValid(cs.getManufacturer()))
			json.put("manufacturer", cs.getManufacturer());
		if (isValid(cs.getModel()))
			json.put("model", cs.getModel());
		json.put("serialNumber", cs.getSerialNumber());
		return json;
	}

	private static JSONObject getBaseboard(ComputerSystem cs) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		Baseboard bb = cs.getBaseboard();
		if (isValid(bb.getManufacturer()))
			json.put("manufacturer", bb.getManufacturer());
		if (isValid(bb.getModel()))
			json.put("model", bb.getModel());
		if (isValid(bb.getSerialNumber()))
			json.put("serialNumber", bb.getSerialNumber());
		if (isValid(bb.getVersion()))
			json.put("version", bb.getVersion());
		return json;
	}

	private static JSONObject getFirmware(ComputerSystem cs) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		Firmware fw = cs.getFirmware();
		if (isValid(fw.getDescription()))
			json.put("description", fw.getDescription());
		if (isValid(fw.getManufacturer()))
			json.put("manufacturer", fw.getManufacturer());
		if (isValid(fw.getName()))
			json.put("name", fw.getName());
		if (isValid(fw.getReleaseDate()))
			json.put("releaseDate", fw.getReleaseDate());
		if (isValid(fw.getVersion()))
			json.put("version", fw.getVersion());
		return json;
	}

	private static JSONArray getDiskStores(HardwareAbstractionLayer hal) {
		JSONArray jsonArr = new JSONArray();
		HWDiskStore[] hwDiskStores = hal.getDiskStores();
		for (HWDiskStore hwDiskStore : hwDiskStores) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			json.put("currentQueueLength", hwDiskStore.getCurrentQueueLength());
			if (isValid(hwDiskStore.getModel()))
				json.put("model", hwDiskStore.getModel());
			if (isValid(hwDiskStore.getName()))
				json.put("name", hwDiskStore.getName());
			json.put("partitions", getPartitions(hwDiskStore));
			json.put("readBytes", hwDiskStore.getReadBytes());
			json.put("reads", hwDiskStore.getReads());
			if (isValid(hwDiskStore.getSerial().replace(" ", "")))
				json.put("serial", hwDiskStore.getSerial());
			json.put("size", hwDiskStore.getSize());
			json.put("timeStamp", hwDiskStore.getTimeStamp());
			json.put("transferTime", hwDiskStore.getTransferTime());
			json.put("writeBytes", hwDiskStore.getWriteBytes());
			json.put("writes", hwDiskStore.getWrites());
			jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONArray getPartitions(HWDiskStore hwDiskStore) {
		JSONArray jsonArr = new JSONArray();
		HWPartition[] hwParts = hwDiskStore.getPartitions();
		for (HWPartition hwPart : hwParts) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			if (isValid(hwPart.getIdentification()))
				json.put("identification", hwPart.getIdentification());
			json.put("major", hwPart.getMajor());
			json.put("minor", hwPart.getMinor());
			if (isValid(hwPart.getMountPoint()))
				json.put("mountPoint", hwPart.getMountPoint());
			if (isValid(hwPart.getName()))
				json.put("name", hwPart.getName());
			json.put("size", hwPart.getSize());
			if (isValid(hwPart.getType()))
				json.put("type", hwPart.getType());
			if (isValid(hwPart.getUuid()))
				json.put("uuid", hwPart.getUuid());
			jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONArray getDisplays(HardwareAbstractionLayer hal) {
		JSONArray jsonArr = new JSONArray();
		Display[] displays = hal.getDisplays();
		for (Display display : displays) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			byte[] edid = display.getEdid();
			json.put("hcm", EdidUtil.getHcm(edid));
			json.put("manufacturerID", EdidUtil.getManufacturerID(edid));
			json.put("productID", EdidUtil.getProductID(edid));
			json.put("serialNo", EdidUtil.getSerialNo(edid));
			json.put("vcm", EdidUtil.getVcm(edid));
			json.put("version", EdidUtil.getVersion(edid));
			json.put("week", EdidUtil.getWeek(edid));
			json.put("year", EdidUtil.getYear(edid));
			jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONObject getMemory(HardwareAbstractionLayer hal) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		GlobalMemory gm = hal.getMemory();
		json.put("available", gm.getAvailable());
		json.put("pageSize", gm.getPageSize());
		json.put("swapPagesIn", gm.getSwapPagesIn());
		json.put("swapPagesOut", gm.getSwapPagesOut());
		json.put("swapTotal", gm.getSwapTotal());
		json.put("swapUsed", gm.getSwapUsed());
		json.put("total", gm.getTotal());
		return json;
	}

	private static JSONArray getNetworkIFs(HardwareAbstractionLayer hal) {
		JSONArray jsonArr = new JSONArray();
		NetworkIF[] netIFs = hal.getNetworkIFs();
		for (NetworkIF netIF : netIFs) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			json.put("bytesRecv", netIF.getBytesRecv());
			json.put("bytesSent", netIF.getBytesSent());
			if (isValid(netIF.getDisplayName()))
				json.put("displayName", netIF.getDisplayName());
			json.put("iPv4addr", netIF.getIPv4addr());
			json.put("iPv6addr", netIF.getIPv6addr());
			json.put("inErrors", netIF.getInErrors());
			json.put("mTU", netIF.getMTU());
			json.put("macaddr", netIF.getMacaddr());
			if (isValid(netIF.getName()))
				json.put("name", netIF.getName());
			json.put("outErrors", netIF.getOutErrors());
			json.put("packetsRecv", netIF.getPacketsRecv());
			json.put("packetsSent", netIF.getPacketsSent());
			json.put("speed", netIF.getSpeed());
			json.put("timeStamp", netIF.getTimeStamp());
			jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONArray getPowerSources(HardwareAbstractionLayer hal) {
		JSONArray jsonArr = new JSONArray();
		PowerSource[] pss = hal.getPowerSources();
		for (PowerSource ps : pss) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			if (isValid(ps.getName()))
				json.put("name", ps.getName());
			if (ps.getRemainingCapacity() != 0.0)
				json.put("remainingCapacity", ps.getRemainingCapacity());
			if (ps.getTimeRemaining() != -1.0)
				json.put("timeRemaining", ps.getTimeRemaining());
			if (!json.isEmpty())
				jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONObject getProcessor(HardwareAbstractionLayer hal) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		CentralProcessor cp = hal.getProcessor();
		json.put("contextSwitches", cp.getContextSwitches());
		json.put("cpu64bit", cp.isCpu64bit());
		json.put("family", cp.getFamily());
		json.put("identifier", cp.getIdentifier());
		json.put("interrupts", cp.getInterrupts());
		json.put("logicalProcessorCount", cp.getLogicalProcessorCount());
		json.put("model", cp.getModel());
		json.put("name", cp.getName());
		json.put("physicalPackageCount", cp.getPhysicalPackageCount());
		json.put("physicalProcessorCount", cp.getPhysicalProcessorCount());
		json.put("processorID", cp.getProcessorID());
		json.put("stepping", cp.getStepping());
		json.put("vendor", cp.getVendor());
		json.put("vendorFreq", cp.getVendorFreq());
		return json;
	}

	private static JSONObject getSensors(HardwareAbstractionLayer hal) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		Sensors ss = hal.getSensors();
		if (ss.getCpuTemperature() != 0.0)
			json.put("cpuTemperature", ss.getCpuTemperature());
		if (ss.getCpuVoltage() != 0.0)
			json.put("cpuVoltage", ss.getCpuVoltage());
		if (ss.getFanSpeeds().length > 0)
			json.put("fanSpeeds", ss.getFanSpeeds());
		return json;
	}

	private static JSONArray getSoundCards(HardwareAbstractionLayer hal) {
		JSONArray jsonArr = new JSONArray();
		SoundCard[] scs = hal.getSoundCards();
		for (SoundCard sc : scs) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			if (isValid(sc.getCodec()))
				json.put("codec", sc.getCodec());
			if (isValid(sc.getDriverVersion()))
				json.put("driverVersion", sc.getDriverVersion());
			if (isValid(sc.getName()))
				json.put("name", sc.getName());
			if (!json.isEmpty())
				jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONObject getOperatingSystem(SystemInfo si) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		OperatingSystem os = si.getOperatingSystem();
		json.put("bitness", os.getBitness());
		if (isValid(os.getFamily()))
			json.put("family", os.getFamily());
		json.put("fileSystem", getFileSystem(os));
		if (isValid(os.getManufacturer()))
			json.put("manufacturer", os.getManufacturer());
		json.put("networkParams", getNetworkParams(os));
		json.put("processCount", os.getProcessCount());
		json.put("processId", os.getProcessId());
		json.put("threadCount", os.getThreadCount());
		json.put("version", getOperatingSystemVersion(os));
		return json;
	}

	private static JSONObject getFileSystem(OperatingSystem os) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		FileSystem fs = os.getFileSystem();
		json.put("fileStores", getFileStores(fs));
		json.put("maxFileDescriptors", fs.getMaxFileDescriptors());
		json.put("openFileDescriptors", fs.getOpenFileDescriptors());
		return json;
	}

	private static JSONArray getFileStores(FileSystem fs) {
		JSONArray jsonArr = new JSONArray();
		OSFileStore[] arrFStor = fs.getFileStores();
		for (OSFileStore fstor : arrFStor) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			if (isValid(fstor.getDescription()))
				json.put("description", fstor.getDescription());
			if (isValid(fstor.getLogicalVolume()))
				json.put("logicalVolume", fstor.getLogicalVolume());
			if (isValid(fstor.getMount()))
				json.put("mount", fstor.getMount());
			if (isValid(fstor.getName()))
				json.put("name", fstor.getName());
			json.put("totalSpace", fstor.getTotalSpace());
			json.put("type", fstor.getType());
			if (isValid(fstor.getUUID()))
				json.put("uUID", fstor.getUUID());
			json.put("usableSpace", fstor.getUsableSpace());
			if (isValid(fstor.getVolume()))
				json.put("volume", fstor.getVolume());
			jsonArr.add(json);
		}
		return jsonArr;
	}

	private static JSONObject getNetworkParams(OperatingSystem os) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		NetworkParams np = os.getNetworkParams();
		json.put("dnsServers", np.getDnsServers());
		if (isValid(np.getDomainName()))
			json.put("domainName", np.getDomainName());
		if (isValid(np.getHostName()))
			json.put("hostName", np.getHostName());
		json.put("ipv4DefaultGateway", np.getIpv4DefaultGateway());
		if (isValid(np.getIpv6DefaultGateway()))
			json.put("ipv6DefaultGateway", np.getIpv6DefaultGateway());
		return json;
	}

	private static JSONObject getOperatingSystemVersion(OperatingSystem os) {
		JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
		OperatingSystemVersion osv = os.getVersion();
		if (isValid(osv.getBuildNumber()))
			json.put("buildNumber", osv.getBuildNumber());
		if (isValid(osv.getCodeName()))
			json.put("codeName", osv.getCodeName());
		if (isValid(osv.getVersion()))
			json.put("version", osv.getVersion());
		return json;
	}

	public static void main(String[] args) {
//		logger.info(JSON.toJSONString(getSystemInfo(), true));
		getProcGrpStat();
	}

	/**
	 * Get group statistics of the processes, group by name. If you need 
	 * to further split, you can re-statistics in the database according 
	 * to the detailed data.
	 */
	private static void getProcGrpStat() {
		OperatingSystem os = new WindowsOperatingSystem();
		JSONArray jsonArr = new JSONArray();
		OSProcess[] pList = os.getProcesses(100, ProcessSort.MEMORY);
		for (OSProcess p : pList) {
			JSONObject json = new JSONObject(new LinkedHashMap<String, Object>());
			json.put("name", p.getName());
			json.put("commandLine", p.getCommandLine());
			json.put("cpuPercent", p.calculateCpuPercent());
			json.put("bytesRead", p.getBytesRead());
			jsonArr.add(json);
		}
		System.out.println(JSON.toJSONString(jsonArr, true));
	}
}
