package org.ayakaji.json;

import java.util.LinkedHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.alibaba.fastjson.support.spring.PropertyPreFilters.MySimplePropertyPreFilter;

public class JsonUtil {
	/**
	 * Make Serialize Include Filter
	 * 
	 * @param props
	 * @return
	 */
	public static final MySimplePropertyPreFilter getIncludeFilter(String[] props) {
		PropertyPreFilters filters = new PropertyPreFilters();
		MySimplePropertyPreFilter includeFilter = filters.addFilter();
		includeFilter.addIncludes(props);
		return includeFilter;
	}

	/**
	 * Make Serialize Exclude Filter
	 * 
	 * @param props
	 * @return
	 */
	public static final MySimplePropertyPreFilter getExcludeFilter(String[] props) {
		PropertyPreFilters filters = new PropertyPreFilters();
		MySimplePropertyPreFilter excludeFilter = filters.addFilter();
		excludeFilter.addExcludes(props);
		return excludeFilter;
	}

	/**
	 * Merge many jsons into one big json
	 * 
	 * @param jsons
	 * @return
	 */
	public static String merge(String[] jsons) {
		if (jsons == null || jsons.length == 0)
			return null;
		JSONObject result = JSON.parseObject(jsons[0]);
		for (int i = 1; i < jsons.length; i++) {
			result.putAll(JSON.parseObject(jsons[i]));
		}
		return JSON.toJSONString(result, SerializerFeature.PrettyFormat);
	}

//	public static JSONObject merge(JSONObject a, JSONObject b) {
//		JSONObject r = new JSONObject(new LinkedHashMap<String, Object>());
//		for (String ka : a.keySet()) {
//			r.put(ka, a.get(ka));
//		}
//		for (String kb : b.keySet()) {
//			if (r.keySet().contains(kb)) {
//				if (r.get(kb) instanceof String && b.get(kb) instanceof String) {
//					if (!((String) r.get(kb)).equals((String) b.get(kb))) {
//						r.put(kb, new String[] {(String) r.get(kb), (String) b.get(kb)});
//					}
//				}
//			} else {
//				r.put(kb, b.get(kb));
//			}
//		}
//	}
}
