package org.ayakaji.verify;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.profesorfalken.wmi4java.WMI4Java;

public class WmiVerifier {
	
	private final static Logger logger = Logger.getLogger(WmiVerifier.class.getName());
	private final static WMI4Java wmi = WMI4Java.get();

	public static void main(String[] args) {
//		Map<String, String> wmiObjectProperties = WMI4Java.get().computerName(".").namespace("root/cimv2").getWMIObject("Win32_BIOS");
//		logger.info(JSON.toJSONString(wmiObjectProperties, true));
		logger.info(JSON.toJSONString(wmi.getWMIObject("Win32_ComputerSystemProduct"), true));
		logger.info(JSON.toJSONString(wmi.getWMIObject("Win32_BaseBoard"), true));
	}
}
