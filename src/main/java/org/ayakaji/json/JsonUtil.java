package org.ayakaji.json;

import com.alibaba.fastjson.JSON;
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
}
