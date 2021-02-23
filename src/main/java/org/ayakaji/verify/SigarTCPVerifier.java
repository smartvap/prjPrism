package org.ayakaji.verify;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Tcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class SigarTCPVerifier {

	public static void main(String[] args) throws SigarException {
		Sigar sigar = new Sigar();
		Tcp tcp = sigar.getTcp();
		NetConnection[] ncs = sigar.getNetConnectionList(NetFlags.CONN_CLIENT | NetFlags.CONN_TCP);
		JSONArray jsonArr = new JSONArray();
		for (NetConnection nc : ncs) {
			jsonArr.add(JSON.parse(JSON.toJSONString(nc)));
		}
		System.out.println(JSON.toJSONString(jsonArr, true));
	}

}
