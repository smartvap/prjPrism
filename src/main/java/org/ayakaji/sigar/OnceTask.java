/*******************************************************
 * A one-time task for collecting and sending software *
 * and hardware information that is not necessary for  *
 * repeated reporting.                                 *
 ******************************************************/
package org.ayakaji.sigar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.ayakaji.json.JsonUtil;
import org.ayakaji.network.netty.UdpClient;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class OnceTask extends Task {
	/**
	 * Schedules the specified task for execution after the specified delay,
	 * corresponds to the delay parameter of java.util.Timer.schedule.
	 */
	public static final long delayMillis = 2000;
	
	/**
	 * The include or exclude filters for json properties.
	 */
	private static final String[] osFilter = new String[] { "os", "arch", "description", "version" };
	private static final String[] cpuInfoFilter = new String[] { "cpuInfo", "mhz", "model", "totalCores" };
	private static final String[] sysPropsFilter = new String[] { "sysProps", "java.vm.name", "user.dir",
			"java.runtime.version", "user.name" };
	private static final String[] sysEnvFilter = new String[] { "sysEnv" };
	private static final String[] netCfgFilter = new String[] { "net", "address", "description", "name", "broadcast",
			"netmask" };

	@Override
	public void run() {
		UdpClient.send(JsonUtil.merge(new String[] { getOSInfo(), getCpuInfo(), getSysProps(), getSysEnv(), getNetCfg() }));
	}
	
	public static void main(String[] argv) {
		System.out.println(getOSInfo());
	}
	
	/**
	 * Get Computer System Product IdentifyNumber
	 * @return
	 */
	public static String getComputerId() {
		return null;
	}

	private static String getOSInfo() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		OperatingSystem os = OperatingSystem.getInstance();
		jsonObj.put("os", os);
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(osFilter), SerializerFeature.PrettyFormat);
	}

	private static String getCpuInfo() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		Sigar sigar = new Sigar();
		try {
			CpuInfo[] cpuInfoArr = sigar.getCpuInfoList();
			if (cpuInfoArr != null && cpuInfoArr.length > 0)
				jsonObj.put("cpuInfo", cpuInfoArr[0]);
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(cpuInfoFilter), SerializerFeature.PrettyFormat);
	}

	private static String getSysProps() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("sysProps", System.getProperties());
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(sysPropsFilter), SerializerFeature.PrettyFormat);
	}

	private static String getSysEnv() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("sysEnv", System.getenv());
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(sysEnvFilter), SerializerFeature.PrettyFormat);
	}

	private static String getNetCfg() {
		JSONObject jsonObj = new JSONObject();
		List<NetInterfaceConfig> netCfgList = new ArrayList<NetInterfaceConfig>();
		Sigar sigar = new Sigar();
		String[] ifNames = null;
		try {
			ifNames = sigar.getNetInterfaceList();
		} catch (SigarException e) {
			e.printStackTrace();
		}
		for (int i = 0; ifNames != null && i < ifNames.length; i++) {
			NetInterfaceConfig ifcfg = null;
			try {
				ifcfg = sigar.getNetInterfaceConfig(ifNames[i]);
			} catch (SigarException e) {
				e.printStackTrace();
			}
			if (ifcfg != null && !ifcfg.getAddress().equals("0.0.0.0") && !ifcfg.getAddress().equals("127.0.0.1")) {
				netCfgList.add(ifcfg);
			}
		}
		jsonObj.put("net", netCfgList);
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(netCfgFilter), SerializerFeature.PrettyFormat);
	}

}