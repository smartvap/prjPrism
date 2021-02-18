/******************
 * Out of the box *
 *****************/
package org.ayakaji.otb;

import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;

public class OtbWrapper {
	private static Logger LOGGER = Logger.getLogger(OtbWrapper.class.getName());
	
	private final static String osName;
	static {
		osName = System.getProperty("os.name");
	}

	public static void main(String[] args) {
		LOGGER.info(JSON.toJSONString(System.getProperties(), true));
//		LOGGER.info(JSON.toJSONString(System.getenv(), true));
	}

}
