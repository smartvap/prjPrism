package org.ayakaji.verify;

import java.util.HashMap;
import java.util.Map;

import com.profesorfalken.jpowershell.PowerShell;

public class PowerShellVerifier {

	private static final Map<String, String> mapPwrShlCfg = new HashMap<String, String>() {
		private static final long serialVersionUID = 886116597971831877L;
		{
			put("maxWait", "60000");
		}
	};

	private final static PowerShell pwrShl = PowerShell.openSession().configuration(mapPwrShlCfg);
	private final static String sysId = pwrShl.executeCommand("(Get-WmiObject Win32_ComputerSystemProduct).IdentifyingNumber").getCommandOutput();
	private final static String sysName = pwrShl.executeCommand("(Get-WmiObject Win32_ComputerSystemProduct).Name").getCommandOutput();
	private final static String sysVendor = pwrShl.executeCommand("(Get-WmiObject Win32_ComputerSystemProduct).Vendor").getCommandOutput();
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				pwrShl.close();
			}
		});
	}

	public static void main(String[] args) throws InterruptedException {
		
		
	}
}
