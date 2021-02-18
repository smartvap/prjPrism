package org.ayakaji.sigar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ayakaji.json.JsonUtil;
import org.ayakaji.network.netty.UdpClient;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class CycleTask extends Task {
	
	public static final int deplayMillis = 1000;

	/**
	 * 
	 */
	public static final int period = 5000;
	private static final String[] memFilter = new String[] { "mem", "used", "free", "total", "usedPercent" };

	@Override
	public void run() {
		UdpClient.send(JsonUtil.merge(new String[] { getMem(), getSwap(), getCpuPerc(), getWho(), getFsUsage() }));
	}

	private static String getMem() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		Sigar sigar = new Sigar();
		try {
			jsonObj.put("mem", sigar.getMem());
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(jsonObj, JsonUtil.getIncludeFilter(memFilter), SerializerFeature.PrettyFormat);
	}

	private static String getSwap() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		Sigar sigar = new Sigar();
		try {
			jsonObj.put("swap", sigar.getSwap());
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat);
	}

	private static String getCpuPerc() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		Sigar sigar = new Sigar();
		try {
			jsonObj.put("cpuPerc", sigar.getCpuPerc());
			jsonObj.put("cpuPercList", sigar.getCpuPercList());
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat);
	}

	private static String getWho() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		Sigar sigar = new Sigar();
		try {
			jsonObj.put("who", sigar.getWhoList());
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat);
	}

	private static String getFsUsage() {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		List<Map<String, FileSystemUsage>> fsUsageList = new ArrayList<Map<String, FileSystemUsage>>();
		Sigar sigar = new Sigar();
		FileSystem[] fsList = null;
		try {
			fsList = sigar.getFileSystemList();
		} catch (SigarException e) {
			e.printStackTrace();
		}
		for (int i = 0; fsList != null && i < fsList.length; i++) {
			if (fsList[i].getType() == 2)
				try {
					Map<String, FileSystemUsage> map = new HashMap<String, FileSystemUsage>();
					map.put(fsList[i].getDirName(), sigar.getFileSystemUsage(fsList[i].getDirName()));
					fsUsageList.add(map);
				} catch (SigarException e) {
					e.printStackTrace();
				}
		}
		jsonObj.put("fsUsage", fsUsageList);
		return JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat);
	}
}