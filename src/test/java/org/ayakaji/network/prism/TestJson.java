package org.ayakaji.network.prism;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

public class TestJson {

    @Test
    public void testJsonStrBuild() {
        System.out.println("------------初始化--------------");
        JSONObject obj = new JSONObject();
        obj.put("cloudUserName", "bobo");
        obj.put("password", "test123456");
        System.out.println(obj.toJSONString());
        System.out.println("----------动态赋值--------------");
        obj.put("cloudUserName", "testUser");
        obj.put("password", "24678");
        System.out.println(obj.toJSONString());
        System.out.println("----------变量赋值-------------");
        String userName = "logincode";
        String password = "password";
        obj.put("cloudUserName", userName);
        obj.put("password", password);
        System.out.println(obj.toJSONString());
    }

    @Test
    public void testBuildDynaimcJson() {
        String requestBody = "{\"cloudUserName\":\"bobo\",\"password\":\"test123456\"}";
        System.out.println(requestBody);
        JSONObject obj = new JSONObject();
        obj.put("password", "test123456");
        obj.put("cloudUserName", "bobo");
        String json = obj.toJSONString();
        System.out.println(json);
        json.replaceAll("\"", "\\\"");
        System.out.println(json);
    }

    @Test
    public void testBuildDynamicJson2() {
        String userName = "bobo";
        String pwd = "test123456";
        String requestBody = "{\"cloudUserName\":\"" + userName + "\",\"password\":\"" + pwd + "\"}";
        System.out.println(requestBody);
    }
}
