package org.ayakaji.verify;

import java.util.Arrays;
import java.util.LinkedHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JSONVerifier {

	public static void main(String[] args) {
//		JSONObject j = new JSONObject(new LinkedHashMap<String, Object>());
//		j.put("a", new String[] {"b", "c"});
//		System.out.println(JSON.toJSONString(j, true));
		String json = "{ \"c\" : \"d\" }";
		System.out.println(((JSONObject) JSONObject.parse(json)).get("c").getClass());
	}

}
