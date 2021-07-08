package org.ayakaji.reporting;

import org.ayakaji.elasticsearch.ElasticSearchPoolUtil;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;
import sun.awt.windows.WPrinterJob;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestCiscoReporting {

    @Test
    public void testCdpNeighborsMsgReporting() throws Exception {
        String[] neighborsStrArr = buildCdpNeighborsCmdResult();


        String readFlag = "Device-ID";
        List<String[]> items = new ArrayList<String[]>();
        boolean beginRead = false;
        for (int i = 0; i < neighborsStrArr.length; i++) {
            String line = neighborsStrArr[i];
            if (line.contains(readFlag)) {
                beginRead = true;
            }
            if (!beginRead) {
                continue;
            }
            List<String> properties = new ArrayList<String>();
//            开始解析
            char[] lineChars = line.trim().toCharArray();
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < lineChars.length; j++) {
                char curChar = lineChars[j];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
                if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                    int split = curChar == 9 ? 2 : 0;
                    for (; j < lineChars.length && (lineChars[j] == 32 || lineChars[j] == 9 || lineChars[j] == 0xa0
                            || lineChars[j] == 0xc2); j++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                    j--;
                    if (split < 2) {
                        builder.append(curChar);
                    } else {
                        String property = builder.toString().trim();
                        properties.add(property);
                        builder = new StringBuilder();
                    }
                } else {
                    builder.append(curChar);
                }
            }
            if (builder.length() > 0) {
                String property = builder.toString().trim();
                properties.add(property);
                builder = new StringBuilder();
            }
            String[] cols = properties.toArray(new String[properties.size()]);
            items.add(cols);
//            System.out.println();
        }

        XContentBuilder xbuilder = XContentFactory.jsonBuilder();
        xbuilder.startObject();
        xbuilder.field("hostName", "JS-DC01-N7K-1-Access");
        xbuilder.field("ipAddr", "10.19.194.134");
        xbuilder.field("detectTime", new Date());
        xbuilder.startArray("cdpNeighbors");
        String[] titles = items.get(0);
        for (int i = 0; i < titles.length; i++) {
            titles[i] = titles[i].trim().replaceAll(" ", "_").toLowerCase();
        }
        for (int i = 1, len = items.size(); i < len; i++) {
            String[] item = items.get(i);
            xbuilder.startObject();
            for (int j = 0; j < item.length; j++) {
                xbuilder.field(titles[j], item[j]);
            }
            xbuilder.endObject();
        }
        xbuilder.endArray();
        xbuilder.endObject();
        String key = String.valueOf(Calendar.getInstance().getTimeInMillis());
        IndexRequest request = new IndexRequest("cisco_neighbors").id(key).source(xbuilder).
                timeout(TimeValue.timeValueSeconds(3));
        RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
        client.index(request, RequestOptions.DEFAULT);
        ElasticSearchPoolUtil.returnClient(client);
    }

    private String[] buildCdpNeighborsCmdResult() {
        String[] model = new String[12];
        model[0] = "JS-DC01-N7K-1-Access# show cdp neighbors ";
        model[1] = "Capability Codes: R - Router, T - Trans-Bridge, B - Source-Route-Bridge";
        model[2] = "                  S - Switch, H - Host, I - IGMP, r - Repeater,";
        model[3] = "                  V - VoIP-Phone, D - Remotely-Managed-Device,";
        model[4] = "                  s - Supports-STP-Dispute";
        model[5] = "";
        model[6] = "Device-ID          Local Intrfce  Hldtme    Capability  Platform      Port ID";
        model[7] = "GQT-DC02-N7K-1-Access(JAF1638AABD)  Eth1/1         131    R S I s   N7K-C7010     Eth1/1 ";
        model[8] = "JS-DC01-C4507R-JingFen  Eth1/24        158    R S I     WS-C4507R-E   Gig1/23 ";
        model[9] = "JS-DC01-C6509-1-Kernel.sdboss.com   Eth1/28        136    R S I     WS-C6509-E    Ten7/2 ";
        model[10] = "JS-DC01-N7K-2-Access(JAF1811AGEE)  Eth1/39        131    R S I s   N7K-C7010     Eth1/39";
        model[11] = "JS-DC01-N7K-2-Access(JAF1811AGEE)	Eth1/40        131    R S I s   N7K-C7010     Eth1/40";
        return model;
    }

    /**
     * 解析结果文本中的指标信息
     * @param context 一整行的命令结果信息
     * @return 有效信息的属性数组
     */
    private List<String> parseProperties (String context) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2) {
                    builder.append(curChar);
                } else {
                    String property = builder.toString().trim();
                    properties.add(property);
                    builder = new StringBuilder();
                }
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }
    /**
     * 解析结果文本中的指标信息
     * @param context 一整行的命令结果信息
     * @return 有效信息的属性数组
     */
    private List<String> strictParseProperties (String context) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                String property = builder.toString().trim();
                properties.add(property);
                builder = new StringBuilder();
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }

    @Test
    public void testShowModuleResultAnalyze() {
        String[] lines = buildShowModuleResultArr();

        List<List<String>> modList = new ArrayList<List<String>>();
        List<List<String>> xbarList = new ArrayList<List<String>>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("Mod")) {
                i++;
                List<String> colTitles = parseProperties(line);
                if (modList.isEmpty()) {
                    modList.add(colTitles);
                } else {
                    colTitles.remove(0);
                    modList.get(0).addAll(colTitles);
                }
                int dataLineNum = 0;
                for (; i < lines.length; i++, dataLineNum++) {
                    String realLine = lines[i];
                    if (realLine.startsWith("--") && realLine.endsWith("--")) {
                        continue;
                    }
//                    如果是空行，读取结束
                    if (realLine.trim().isEmpty()) {
                        break;
                    }
                    List<String> properties = parseProperties(realLine);
                    if (modList.size() < dataLineNum + 1) {
                        modList.add(properties);
                    } else {
                        properties.remove(0);
                        modList.get(dataLineNum).addAll(properties);
                    }
                }
            } else if (line.startsWith("Xbar")) {
                i++;
                List<String> colTitles = strictParseProperties(line);
                if (xbarList.isEmpty()) {
                    xbarList.add(colTitles);
                } else {
                    colTitles.remove(0);
                    xbarList.get(0).addAll(colTitles);
                }
                int dataLineNum = 0;
                for (; i < lines.length; i++, dataLineNum++) {
                    String realLine = lines[i];
                    if (realLine.startsWith("--") && realLine.endsWith("--")) {
                        continue;
                    }
//                    如果是空行，读取结束
                    if (realLine.trim().isEmpty()) {
                        break;
                    }
                    List<String> properties = parseProperties(realLine);
                    if (xbarList.size() < dataLineNum + 1) {
                        xbarList.add(properties);
                    } else {
                        properties.remove(0);
                        xbarList.get(dataLineNum).addAll(properties);
                    }
                }
            }
        }

        modList.forEach(subList -> {
            String[] arr = subList.toArray(new String[subList.size()]);
            System.out.println(String.join("|", arr));
        });

        System.out.println("\n\n");

        xbarList.forEach(subList -> {
            String[] arr = subList.toArray(new String[subList.size()]);
            System.out.println(String.join("|", arr));
        });
    }




    private String[] buildShowModuleResultArr() {
        List<String> builderList = new ArrayList<String>();
        builderList.add("show environment power");
        builderList.add("Mod  Ports  Module-Type                         Model              Status");
        builderList.add("---  -----  ----------------------------------- ------------------ ----------");
        builderList.add("1    48     1/10 Gbps Ethernet Module           N7K-F248XP-25E     ok");
        builderList.add("5    0      Supervisor Module-2                 N7K-SUP2E          active *");
        builderList.add("6    0      Supervisor Module-2                 N7K-SUP2E          ha-standby");
        builderList.add("7    48     1/10 Gbps Ethernet Module           N7K-F248XP-25E     ok");
        builderList.add("");
        builderList.add("Mod  Sw              Hw");
        builderList.add("---  --------------  ------");
        builderList.add("1    6.2(16)         1.0     ");
        builderList.add("5    6.2(16)         6.0     ");
        builderList.add("6    6.2(16)         6.0     ");
        builderList.add("7    6.2(16)         1.0     ");
        builderList.add("");
        builderList.add("");
        builderList.add("");
        builderList.add("Mod  MAC-Address(es)                         Serial-Num");
        builderList.add("---  --------------------------------------  ----------");
        builderList.add("1    e4-c7-22-15-9d-54 to e4-c7-22-15-9d-87  JAF1746AFHA");
        builderList.add("5    e4-c7-22-1b-37-b4 to e4-c7-22-1b-37-c6  JAF1810AHFN");
        builderList.add("6    e4-c7-22-1c-d6-4a to e4-c7-22-1c-d6-5c  JAF1813APMB");
        builderList.add("7    e4-c7-22-18-28-c0 to e4-c7-22-18-28-f3  JAF1750BDRC");
        builderList.add("");
        builderList.add("Mod  Online Diag Status");
        builderList.add("---  ------------------");
        builderList.add("1    Pass");
        builderList.add("5    Pass");
        builderList.add("6    Pass");
        builderList.add("7    Pass");
        builderList.add("");
        builderList.add("Xbar Ports  Module-Type                         Model              Status");
        builderList.add("---  -----  ----------------------------------- ------------------ ----------");
        builderList.add("1    0      Fabric Module 2                     N7K-C7010-FAB-2    ok");
        builderList.add("2    0      Fabric Module 2                     N7K-C7010-FAB-2    ok");
        builderList.add("3    0      Fabric Module 2                     N7K-C7010-FAB-2    ok");
        builderList.add("4    0      Fabric Module 2                     N7K-C7010-FAB-2    ok");
        builderList.add("5    0      Fabric Module 2                     N7K-C7010-FAB-2    ok");
        builderList.add("");
        builderList.add("Xbar Sw              Hw");
        builderList.add("---  --------------  ------");
        builderList.add("1    NA              1.6     ");
        builderList.add("2    NA              1.6     ");
        builderList.add("3    NA              1.6     ");
        builderList.add("4    NA              1.6     ");
        builderList.add("5    NA              1.6     ");
        builderList.add("");
        builderList.add("");
        builderList.add("");
        builderList.add("Xbar MAC-Address(es)                         Serial-Num");
        builderList.add("---  --------------------------------------  ----------");
        builderList.add("1    NA                                      JAF1814AHLL");
        builderList.add("2    NA                                      JAF1815AMGA");
        builderList.add("3    NA                                      JAF1814AHQC");
        builderList.add("4    NA                                      JAF1814AHLB");
        builderList.add("5    NA                                      JAF1814AHKJ");
        builderList.add("");
        builderList.add("* this terminal session ");
        String[] lines = builderList.toArray(new String[builderList.size()]);
        return lines;
    }

    /**
     * 测试 show redundancy status 结果解析
     */
    @Test
    public void testShowRedundancyStatusResultAnalyze () {

        String thisSupervisorState = null;
        String otherSupervisorState = null;
        String thisSupervisorInternalState = null;
        String otherSupervisorInternalState = null;
        boolean restartRecently = false;
        String target = null;
        Pattern pattern = Pattern.compile("[\\s\\S]*0[^\\d]*day[\\s\\S]*");

        String[] lines = buildShowRedundancyStatusResult();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("This supervisor")) {
                target = "This supervisor";
                continue;
            }
            if (line.contains("Other supervisor")) {
                target = "Other supervisor";
                i += 3;
                continue;
            }
            if (line == null || line.isEmpty() || line.trim().isEmpty() || line.startsWith("---")
                    && line.endsWith("---")) {
                continue;
            }

            if (line.contains("Supervisor state")) {
                String[] entry = line.split(":");
                String val = entry[1].trim();
                if ("This supervisor".equals(target)) {
                    thisSupervisorState = val;
                } else {
                    otherSupervisorState = val;
                }
                continue;
            }

            if (line.contains("Internal state:")) {
                String[] entry = line.split(":");
                String val = entry[1].trim();
                if ("This supervisor".equals(target)) {
                    thisSupervisorInternalState = val;
                } else {
                    otherSupervisorInternalState = val;
                }
                continue;
            }


            if (line.contains("System uptime:") || line.contains("Kernel uptime:") ||
                    line.contains("Active supervisor uptime:")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    System.out.println("error line:\t" + line);
                }
            }

        }

        System.out.println(String.join("\t", "this state", "this internal", "other state", "other internal"));
        System.out.println(String.join("\t", thisSupervisorState, thisSupervisorInternalState, otherSupervisorState,
                otherSupervisorInternalState));

        IndexRequest request = new IndexRequest("cisco_redundancy_status");
        try {
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder();
            contentBuilder.startObject();
            contentBuilder.startObject("thisSupervisor");
            {
                contentBuilder.field("state", thisSupervisorState);
                contentBuilder.field("internalState", thisSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.startObject("otherSupervisor");
            {
                contentBuilder.field("state", otherSupervisorState);
                contentBuilder.field("internalState", otherSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.endObject();
            RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
            request.id("js-dc01-n7k-1-access");
            request.source(contentBuilder);
            request.timeout(TimeValue.timeValueSeconds(3));
            client.index(request, RequestOptions.DEFAULT);
            ElasticSearchPoolUtil.returnClient(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] buildShowRedundancyStatusResult () {
        List<String> arrL = new ArrayList<String>();
        arrL.add("Redundancy mode");
        arrL.add("---------------");
        arrL.add("      administrative:   HA");
        arrL.add("         operational:   HA");
        arrL.add("");
        arrL.add("This supervisor (sup-5)");
        arrL.add("-----------------------");
        arrL.add("    Redundancy state:   N/A");
        arrL.add("    Supervisor state:   Active");
        arrL.add("      Internal state:   Active with HA standby");
        arrL.add("");
        arrL.add("Other supervisor (sup-6)");
        arrL.add("------------------------");
        arrL.add("    Redundancy state:   N/A");
        arrL.add("");
        arrL.add("    Supervisor state:   HA standby");
        arrL.add("      Internal state:   HA standby");
        arrL.add("");
        arrL.add("System start time:          Tue Jan  7 14:56:36 2020");
        arrL.add("");
        arrL.add("System uptime:              0 days, 23 hours, 47 minutes, 37 seconds");
        arrL.add("Kernel uptime:              461 days, 23 hours, 53 minutes, 51 seconds");
        arrL.add("Active supervisor uptime:   461 days, 23 hours, 47 minutes, 37 seconds");
        return arrL.toArray(new String[arrL.size()]);
    }

    @Test
    public void testComparinghistoryRedundancyInfo() throws Exception {
        String thisSupervisorState = "Active";
        String otherSupervisorState = "Active with HA standby";
        String thisSupervisorInternalState = "HA standby";
        String otherSupervisorInternalState = "HA standby";

        RestHighLevelClient client = ElasticSearchPoolUtil.getClient();
        GetRequest finder = new GetRequest("cisco_redundancy_status", "js-dc01-n7k-1-access");
        GetResponse historyRecord = client.get(finder, RequestOptions.DEFAULT);
        ElasticSearchPoolUtil.returnClient(client);
        Map<String, String> thisSupervisor = (Map<String, String>)historyRecord.getSource().get("thisSupervisor");
        Map<String, String> otherSupervisor = (Map<String, String>)historyRecord.getSource().get("otherSupervisor");
        System.out.println(thisSupervisor.get("state"));
        System.out.println(thisSupervisor.get("internalState"));
        System.out.println(otherSupervisor.get("state"));
        System.out.println(otherSupervisor.get("internalState"));
    }

    @Test
    public void updateSupervisorStateInfo() {
        String hostName = "JS-DC01-N7K-1-ACCESS";
        String thisSupervisorState = "error";
        String thisSupervisorInternalState = "error";
        String otherSupervisorState = "error";
        String otherSupervisorInternalState = "error";
        String statusId = hostName.toLowerCase().replaceAll(" ", "_");
        RestHighLevelClient client = null;
        try {
            IndexRequest updateRequest = new IndexRequest("cisco_redundancy_status").id(statusId);
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder();
            contentBuilder.startObject();
            contentBuilder.startObject("thisSupervisor");
            {
                contentBuilder.field("state", thisSupervisorState);
                contentBuilder.field("internalState", thisSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.startObject("otherSupervisor");
            {
                contentBuilder.field("state", otherSupervisorState);
                contentBuilder.field("internalState", otherSupervisorInternalState);
            }
            contentBuilder.endObject();
            contentBuilder.endObject();
            updateRequest.source(contentBuilder);
            updateRequest.timeout(TimeValue.timeValueSeconds(3));
            client = ElasticSearchPoolUtil.getClient();
            client.index(updateRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                ElasticSearchPoolUtil.returnClient(client);
            }
        }
    }

    @Test
    public void testAnalyzeShowEnvironmentPowerResult() {
        String[] lines = buildShowEnvironmentPowerResult();
        boolean beginRead = false;
        String target = null;
        List<String[]> powerSupply = new ArrayList<String[]>();
        List<String[]> moduleInfo = new ArrayList<String[]>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if(line.startsWith("---") && line.endsWith("---")) {
//                读表头, 并确定这次是读哪一组的信息
                String title = lines[i - 2];
                if (title == null || title.isEmpty() || title.trim().isEmpty()) {
                    continue;
                }
                beginRead = true;
                List<String> titles = parseProperties(title);
                String[] titleArr = titles.toArray(new String[0]);
                String firTitle = titleArr[0];
                if (firTitle.contains("Supply")) {
                    target = "power supply";
                    powerSupply.add(titleArr);
                } else {
                    target = "module";
                    moduleInfo.add(titleArr);
                }

                continue;
            }

            if (line == null || line.trim().isEmpty()) {
                beginRead = false;
                continue;
            }

            if (!beginRead) {
                continue;
            }

//            开始读取指标信息
            String[] props = parseProperties(line).toArray(new String[0]);
            if ("power supply".equalsIgnoreCase(target)) {
                powerSupply.add(props);
            } else {
                moduleInfo.add(props);
            }
        }

//        验证一下：

        /*powerSupply.forEach(item ->{
            System.out.println(String.join("|", item));
        });
        System.out.println("\t\t");
        moduleInfo.forEach(item -> {
            System.out.println(String.join("|", item));
        });*/

        /**
         * 解读采集到的信息
         */
//        规定读取指标的下标，如果是
        int[] readIdxs = new int[3];
        /**
         * 先处理powerSupply段的信息
         */
        String[] titles = powerSupply.get(0);
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            if (title.contains("Supply")) {
                readIdxs[0] = i;
            }
            if (title.contains("Model")) {
                readIdxs[1] = i;
            }
            if (title.contains("Status")) {
                readIdxs[2] = i;
            }
        }
        for (int i = 1, len = powerSupply.size(); i < len; i++) {
            String[] props = powerSupply.get(i);
            String indicatorPrefix = props[readIdxs[0]];
            String indicatorBody = props[readIdxs[1]];
            String indicatorValue = props[readIdxs[2]];
            String indicatorName = String.join("_", indicatorPrefix, indicatorBody);
            System.out.println(String.join(":", indicatorName, indicatorValue));
        }

        /**
         * 再处理moduleInfo的采集结果信息
         */
        titles = moduleInfo.get(0);
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            if ("Module".equalsIgnoreCase(title)) {
                readIdxs[0] = i;
            }
            if ("Model".equalsIgnoreCase(title)) {
                readIdxs[1] = i;
            }
            if ("Status".equalsIgnoreCase(title)) {
                readIdxs[2] = i;
            }
        }
        for (int i = 1, len = moduleInfo.size(); i < len; i++) {
            String[] props = moduleInfo.get(i);
            String indicatorPrefix = props[readIdxs[0]];
            String indicatorBody = props[readIdxs[1]];
            String indicatorValue = props[readIdxs[2]];
            String indicatorName = String.join(indicatorPrefix, indicatorBody);
            System.out.println(String.join(":", indicatorName, indicatorValue));
        }

    }

    private String[] buildShowEnvironmentPowerResult() {
        String[] lines = new String[]{
                "Power Supply:",
                "Voltage: 50 Volts",
                "Power                              Actual        Total",
                "Supply    Model                    Output     Capacity    Status",
                "                                 (Watts )     (Watts )",
                "-------  -------------------  -----------  -----------  --------------",
                "1        N7K-AC-6.0KW               341 W       6000 W     Ok        ",
                "2        N7K-AC-6.0KW               475 W       6000 W     Ok        ",
                "3        N7K-AC-6.0KW               337 W       6000 W     Ok        ",
                "",
                "",
                "                                  Actual        Power      ",
                "Module    Model                     Draw    Allocated    Status",
                "                                 (Watts )     (Watts )     ",
                "-------  -------------------  -----------  -----------  --------------",
                "1        N7K-F248XP-25E             314 W        450 W    Powered-Up",
                "5        N7K-SUP2E                  143 W        265 W    Powered-Up",
                "6        N7K-SUP2E                  128 W        265 W    Powered-Up",
                "7        N7K-F248XP-25E             297 W        450 W    Powered-Up",
                "Xb1      N7K-C7010-FAB-2            N/A           80 W    Powered-Up",
                "Xb2      N7K-C7010-FAB-2            N/A           80 W    Powered-Up",
                "Xb3      N7K-C7010-FAB-2            N/A           80 W    Powered-Up",
                "Xb4      N7K-C7010-FAB-2            N/A           80 W    Powered-Up",
                "Xb5      N7K-C7010-FAB-2            N/A           80 W    Powered-Up",
                "fan1     N7K-C7010-FAN-S            198 W        720 W    Powered-Up",
                "fan2     N7K-C7010-FAN-S            198 W        720 W    Powered-Up",
                "fan3     N7K-C7010-FAN-F             16 W        120 W    Powered-Up",
                "fan4     N7K-C7010-FAN-F             16 W        120 W    Powered-Up",
                "",
                "N/A - Per module power not available",
                "",
                "",
                "Power Usage Summary:",
                "--------------------",
                "Power Supply redundancy mode (configured)                Redundant",
                "Power Supply redundancy mode (operational)               Redundant",
                "",
                "Total Power Capacity (based on configured mode)               9000 W",
                "Total Power of all Inputs (cumulative)                       18000 W",
                "Total Power Output (actual draw)                              1153 W",
                "Total Power Allocated (budget)                                3510 W",
                "Total Power Available for additional modules                  5490 W"
        };
        return lines;
    }

    @Test
    public void testAnalyzeShowModuleUptimeResult () {
        String[] lines = buildShowModuleUptime();
        Pattern filterPattern = Pattern.compile("[ \\f\\r\\t\\n]*[\\-]{1,}([^\\-]*)[\\-]{1,}");
        Pattern indicatorPattern = Pattern.compile("Up Time:[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*day[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*hour[s]?,[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*minute[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*second[s]?");
        String indicatorFormat = "%s %s:%s:%s";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher filterMatcher = filterPattern.matcher(line);
            String indicatorName = null;
            if (filterMatcher.find()) {
                indicatorName = filterMatcher.group(1);
                String indicatorValLine = lines[i += 2];
                Matcher valMatcher = indicatorPattern.matcher(indicatorValLine);
                String indicatorValue = null;
                if (valMatcher.find()) {
                    indicatorValue = String.format(indicatorFormat, valMatcher.group(1), valMatcher.group(2),
                            valMatcher.group(3), valMatcher.group(4));
                }
                System.out.println(String.join("\t", indicatorName, indicatorValue));
            }
        }
    }

    private String[] buildShowModuleUptime() {
        String[] lines = new String[] {
                "show processes cpu history",
                "------ Module 1 -----",
                "Module Start Time:    Tue Jan  7 14:56:04 2020",
                "Up Time:             461 days, 23 hours, 48 minutes, 18 seconds",
                "",
                "------ Module 5 -----",
                "Module Start Time:    Tue Jan  7 14:53:02 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 20 seconds",
                "",
                "------ Module 6 -----",
                "Module Start Time:    Tue Jan  7 14:59:07 2020",
                "Up Time:             461 days, 23 hours, 45 minutes, 15 seconds",
                "",
                "------ Module 7 -----",
                "Module Start Time:    Tue Jan  7 14:56:07 2020",
                "Up Time:             461 days, 23 hours, 48 minutes, 15 seconds",
                "",
                "------ Xbar 1 -----",
                "Xbar Start Time:    Tue Jan  7 14:53:12 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 10 seconds",
                "",
                "------ Xbar 2 -----",
                "Xbar Start Time:    Tue Jan  7 14:53:14 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 8 seconds",
                "",
                "------ Xbar 3 -----",
                "Xbar Start Time:    Tue Jan  7 14:53:16 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 6 seconds",
                "",
                "------ Xbar 4 -----",
                "Xbar Start Time:    Tue Jan  7 14:53:18 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 4 seconds",
                "",
                "------ Xbar 5 -----",
                "Xbar Start Time:    Tue Jan  7 14:53:20 2020",
                "Up Time:             461 days, 23 hours, 51 minutes, 2 seconds"
        };
        return lines;
    }

    @Test
    public void testAnalyzeShowEnvironmentFanResult () {
        String[] lines = buildShowEnvironmentFanResult();
        List<String[]> data = new ArrayList<String[]>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("Fan:")) {
                String titleLine = lines[i+=2];
                String[] titles = parseProperties(titleLine).toArray(new String[0]);
                data.add(titles);
                i++;
                continue;
            }

            if (line.contains("Fan Zone Speed:")) {
                break;
            }

            String[] dataItem = parseProperties(line).toArray(new String[0]);
            data.add(dataItem);
        }
//        验证一下
        /*data.forEach(item -> {
            System.out.println(String.join("\t", item));
        });*/
        int[] readColIndexs = new int[2];
        String[] colTitles = data.get(0);
        for (int i = 0; i < colTitles.length; i++) {
            String title = colTitles[i];
            if ("Fan".equalsIgnoreCase(title)) {
                readColIndexs[0] = i;
            }
            if ("Status".equalsIgnoreCase(title)) {
                readColIndexs[1] = i;
            }
        }
        for (int i = 1, len = data.size(); i < len; i++) {
            String[] dataItem = data.get(i);
            String indicatorName = dataItem[readColIndexs[0]];
            String indicatorValue = dataItem[readColIndexs[1]];
            System.out.println(String.join("\t", indicatorName, indicatorValue));
        }
    }

    private String[] buildShowEnvironmentFanResult () {
        String[] lines = new String[] {
                "Fan:",
                "------------------------------------------------------",
                "Fan             Model                Hw         Status",
                "------------------------------------------------------",
                "Fan1(sys_fan1)  N7K-C7010-FAN-S      1.0        Ok  ",
                "Fan2(sys_fan2)  N7K-C7010-FAN-S      1.0        Ok  ",
                "Fan3(fab_fan1)  N7K-C7010-FAN-F      1.0        Ok  ",
                "Fan4(fab_fan2)  N7K-C7010-FAN-F      1.0        Ok  ",
                "Fan_in_PS1      --                   --         Ok             ",
                "Fan_in_PS2      --                   --         Ok             ",
                "Fan_in_PS3      --                   --         Ok             ",
                "Fan Zone Speed: Zone 1: 0x90 Zone 2: 0x68",
                "Fan Air Filter : Present"
        };
        return lines;
    }

    @Test
    public void testAnalyzeshowProcessesCpuResult () {
        String[] lines = buildshowProcessesCpuResult();
        Pattern indicatorPattern = Pattern.compile("CPU utilization for five seconds: ([\\d]*)%/([\\d]*)%; " +
                "one minute: ([\\d]*)%; five minutes: ([\\d]*)%");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = indicatorPattern.matcher(line);
            if (matcher.find()) {
                String format = "5秒平均: %d%%/%s%%, 1分钟平均: %d%%, 5分钟平均: %d%%";
                int fiveSecondsCost1 = Integer.valueOf(matcher.group(1));
                int fiveSecondsCost2 = Integer.valueOf(matcher.group(2));
                int oneMinuteCost = Integer.valueOf(matcher.group(3));
                int fiveMinuteCost = Integer.valueOf(matcher.group(4));
                System.out.println(String.format(format, fiveSecondsCost1, fiveSecondsCost2, oneMinuteCost, fiveMinuteCost));
                break;
            }
        }
    }

    private String[] buildshowProcessesCpuResult () {
        String [] lines = new String[] {
                "CPU utilization for five seconds: 1%/0%; one minute: 1%; five minutes: 1%",
                "PID    Runtime(ms)  Invoked   uSecs  5Sec    1Min    5Min    TTY  Process",
                "-----  -----------  --------  -----  ------  ------  ------  ---  -----------",
                "    1       261630   8142422      0   0.00%   0.00%  0.00%   -    init",
                "    2           30      2192      0   0.00%   0.00%  0.00%   -    kthreadd",
                "    3          910    111079      0   0.00%   0.00%  0.00%   -    migration/0",
                "    4      7243260  532920694      0   0.00%   0.00%  0.00%   -    ksoftirqd/0",
                "    5            0         4      0   0.00%   0.00%  0.00%   -    watchdog/0",
                "    6         2180    271741      0   0.00%   0.00%  0.00%   -    migration/1",
                "    7       286460  22977041      0   0.00%   0.00%  0.00%   -    ksoftirqd/1",
                "    8            0         2      0   0.00%   0.00%  0.00%   -    watchdog/1",
                "    9         1230    141393      0   0.00%   0.00%  0.00%   -    migration/2",
                "   10       699060  105677380      0   0.00%   0.00%  0.00%   -    ksoftirqd/2"
        };
        return lines;
    }

    @Test
    public void testAnalyzeShowSystemResuorceResult() {
        String[] lines = buildShowSystemResourceResult();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("CPU states")) {
                Pattern pattern = Pattern.compile("([\\d]*(.[\\d]*)?)% idle");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String indicator = matcher.group(1);
                    double indicatorVal = Double.valueOf(indicator);
                    System.out.println(String.join(":", indicator.concat("%"), "50%") + " "
                            + (indicatorVal > 50));
                }
            }
            if (line.contains("Memory usage")) {
                Pattern pattern = Pattern.compile("Memory usage:[ \\t\\r\\n\\f]*([\\d]*)K total, [ \\t\\r\\n\\f]*([\\d]*)K used");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    BigDecimal used = new BigDecimal(matcher.group(2));
                    BigDecimal total = new BigDecimal(matcher.group(1));
                    double useRate = used.multiply(new BigDecimal(100)).divide(total,
                            new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
                    System.out.println(useRate + "% " + (useRate > 30));
                }
            }
        }
    }

    private String[] buildShowSystemResourceResult() {
        String[] lines = new String[] {
                "Load average:   1 minute: 0.18   5 minutes: 0.15   15 minutes: 0.11",
                "Processes   :   1247 total, 1 running",
                "CPU states  :   0.42% user,   0.42% kernel,   99.15% idle",
                "        CPU0 states  :   3.00% user,   2.00% kernel,   95.00% idle",
                "        CPU1 states  :   2.72% user,   0.00% kernel,   97.27% idle",
                "        CPU2 states  :   0.00% user,   3.03% kernel,   96.96% idle",
                "        CPU3 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU4 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU5 states  :   0.00% user,   0.00% kernel,   99.99% idle",
                "        CPU6 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU7 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU8 states  :   0.00% user,   0.94% kernel,   99.05% idle",
                "        CPU9 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU10 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU11 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU12 states  :   0.92% user,   0.00% kernel,   99.07% idle",
                "        CPU13 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU14 states  :   0.00% user,   0.00% kernel,   100.00% idle",
                "        CPU15 states  :   0.00% user,   0.91% kernel,   99.08% idle",
                "Memory usage:   32745060K total,   5555916K used,   27189144K free",
                "Current memory status: OK"
        };
        return lines;
    }

    @Test
    public void testAnalyzeDirBootflashResult () {
        String[] lines = buildDirBootflashResult();
        Pattern usedPattern = Pattern.compile("([\\d]*) bytes used");
        Pattern totalPattern = Pattern.compile("([\\d]*) bytes total");
        BigDecimal usedBytes = null;
        BigDecimal totalBytes = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = usedPattern.matcher(line);
            if (matcher.find()) {
                String usedStr = matcher.group(1);
                usedBytes = new BigDecimal(usedStr);
                continue;
            }
            matcher = totalPattern.matcher(line);
            if (matcher.find()) {
                String totalStr = matcher.group(1);
                totalBytes = new BigDecimal(totalStr);
                continue;
            }
        }
        double usedRate = usedBytes.multiply(new BigDecimal(100)).divide(totalBytes,
                new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
        System.out.println(usedRate + "%");
    }

    private String[] buildDirBootflashResult () {
        String[] sup1 = new String[] {
                "      66231    Sep 18 04:11:46 2014  20140918.checkpoint",
                "     908778    Dec 31 03:15:24 2014  bgp_access1",
                "       3363    Aug 11 02:58:25 2014  snapshot_JS-Access1_Aug_10_2014_1858",
                "      88353    Aug 25 09:56:08 2020  str",
                "",
                "Usage for bootflash://sup-1",
                " 1019981824 bytes used",
                "  754274304 bytes free",
                " 1774256128 bytes total"
        };
        String[] sup2 = new String[] {
                "   19119209    Jan 07 10:33:24 2015  show_tech_out.gz",
                "",
                "Usage for bootflash://sup-2",
                " 1036795904 bytes used",
                "  737460224 bytes free",
                " 1774256128 bytes total"
        };
        return sup2;
    }

    @Test
    public void testAnalyzeShowLoggingLogprofileResult() throws IOException, URISyntaxException {

        URL path = TestCiscoReporting.class.getClassLoader().getResource("org/ayakaji/reporting/logPfiles.log");
        Path logPath = Paths.get(path.toURI());
        List<String> lineList = Files.readAllLines(logPath);

        String[] lines = lineList.toArray(new String[lineList.size()]);
        Pattern logLevelPattern = Pattern.compile("%[^-]*-([\\d]{1})-");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("%") && matchedFilter(line)) {
                Matcher matcher = logLevelPattern.matcher(line);
                if (matcher.find()) {
                    String levelStr = matcher.group(1);
                    int level = Integer.valueOf(levelStr);
                    if (level <= 5) {
                        System.out.println(line);
                    }
                }
            }
        }
    }

    private boolean matchedFilter(String source) throws URISyntaxException, IOException {
        URL filterurl = Thread.currentThread().getContextClassLoader().getResource("org/ayakaji/reporting/logfilter.conf");
        Path filterPath = Paths.get(filterurl.toURI());
        List<String> filters = Files.readAllLines(filterPath);
        for (int i = 0, len = filters.size(); i < len; i++) {
            String filter = filters.get(i);
            if (source.contains(filter)) {
                return true;
            }
        }
        return false;
    }


    @Test
    public void analysisIpOspfNeighborsResult () {
        String[] commandResult = buildIpOspfNeighborsResult();
        Map<String, String> indicators = new HashMap<>();
        int addrIdx = -1;
        int upTimeIdx = -1;
        List<String[]> properties = new ArrayList<>();
        Pattern beginReadPattern = Pattern.compile("Total number of neighbors:[ ]*([\\d]*)");
        for (int i = 0, curRow = -2, rowNum = -1; i < commandResult.length && curRow < rowNum; i++) {
            String line = commandResult[i];
            if (line.contains("Total number of neighbors")) {
//                如果符合条件，则正式开始读取
                Matcher matcher = beginReadPattern.matcher(line);
                if (matcher.find()) {
                    String rowNums = matcher.group(1);
                    rowNum = Integer.valueOf(rowNums).intValue();
                    curRow = 0;
                }
                String titleLine = commandResult[++i];
                int[] forcusIdxs = analyzeTitle(titleLine);
                addrIdx = forcusIdxs[0];
                upTimeIdx = forcusIdxs[1];
                continue;
            }
            if (curRow >= 0) {
                List<String> elements = parseProperties(line);
                String upTime = elements.get(upTimeIdx);
                if (upTime.startsWith("0d")) {
                    String address = elements.get(addrIdx);
                    indicators.put(address, upTime);
                }
            }
        }

        indicators.forEach((key, val) -> {
            System.out.println(String.join("\t:\t", key, val));
        });
    }

    private int[] analyzeTitle(String titleLine) {
        int[] forcusIdxs = new int[] {-1, -1};
        List<String> titles = parseProperties(titleLine);
        for (int i = 0, len = titles.size(); i < len; i++) {
            if (Arrays.binarySearch(forcusIdxs, -1) < 0) {
                break;
            }
            String colTitle = titles.get(i);
            if ("Up Time".equalsIgnoreCase(colTitle)) {
                forcusIdxs[1] = i;
                continue;
            }
            if ("Address".equalsIgnoreCase(colTitle)) {
                forcusIdxs[0] = i;
                continue;
            }
        }
        return forcusIdxs;
    }

    private String[] buildIpOspfNeighborsResult() {
        String[] result = new String[] {
                " OSPF Process ID 10 VRF default",
                " Total number of neighbors: 9",
                " Neighbor ID     Pri State            Up Time  Address         Interface",
                " 10.19.93.249      1 FULL/BDR         1y13w    10.19.93.204    Vlan98 ",
                " 10.19.93.250      1 FULL/DR          0d2h    10.19.93.203    Vlan98 ",
                " 10.19.93.253      1 TWOWAY/DROTHER   1y13w    10.19.93.202    Vlan98 ",
                " 10.19.93.245      1 FULL/DR          1y13w    10.19.93.156    Vlan94 ",
                " 10.19.93.246      1 FULL/DROTHER     0d1h    10.19.93.155    Vlan94 ",
                " 10.19.93.253      1 FULL/DROTHER     28w2d    10.19.93.154    Vlan94 ",
                " 10.19.93.251      1 FULL/DROTHER     1y13w    10.19.93.213    Vlan2 ",
                " 10.19.93.252      1 FULL/DROTHER     0d13h    10.19.93.212    Vlan2 ",
                " 10.19.93.253      1 FULL/BDR         0d13w    10.19.93.209    Vlan2 "
        };
        return result;
    }


    @Test
    public void analysisForSpanningTreeResult() {
        String [] lines = buildSpanningTreeResult();
        Pattern occurTimePattern = Pattern.compile("([\\d]*):[\\d]*:[\\d]*");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = occurTimePattern.matcher(line);
            if (matcher.find()) {
                String hourStr = matcher.group(1);
                int hours = Integer.valueOf(hourStr).intValue();
                if (hours < 12) {
                    System.out.println(line);
                }
                System.out.println(matcher.group());
            }
        }
    }

    private String[] buildSpanningTreeResult() {
        String[] result = new String[]{
                "Number of topology changes 20 last change occurred 10981:17:10 ago",
                "  Number of topology changes 72 last change occurred 5230:54:12 ago",
                "  Number of topology changes 90 last change occurred 5230:54:12 ago",
                "  Number of topology changes 11 last change occurred 10981:16:47 ago",
                "  Number of topology changes 11 last change occurred 10981:16:47 ago",
                "  Number of topology changes 11 last change occurred 10981:16:47 ago",
                "  Number of topology changes 11 last change occurred 10981:16:47 ago",
                "  Number of topology changes 20 last change occurred 10981:17:10 ago",
                "  Number of topology changes 20 last change occurred 10981:17:10 ago",
                "  Number of topology changes 11 last change occurred 11:16:47 ago"
        };
        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Test
    public void testBooean() {
        String s1 = new String("ww");
        String s2 = new String("dd");

    }


    @Test
    public void analysisVPCResult() {
        List<String> errorMsg = new ArrayList<>();
        String[] lines = buildVPCResult();
        List<String> formattedLines = Arrays.stream(lines).map(this::formatLine).collect(Collectors.toList());
        String[] hisLines = buildVPCHistoryResult();
        List<String> formattedHisLines = Arrays.stream(hisLines).map(this::formatLine).collect(Collectors.toList());

        String errorFormat = "line at No.%d of current result is different from history, since [%s], current[%s]";

        for (int i = 0, curSize = formattedLines.size(), historySize = formattedHisLines.size(),
             len = Math.max(curSize, historySize); i < len; i++) {
            String curLine = curSize > i ? formattedLines.get(i) : "Empty";
            String hisLine = historySize > i ? formattedHisLines.get(i) : "Empty";
            if (!curLine.equals(hisLine)) {
                String curReal = curSize > i ? lines[i] : "Empty";
                String hisReal = historySize > i ? hisLines[i] : "Empty";
                String errorItem = String.format(errorFormat, i, hisReal, curReal);
                errorMsg.add(errorItem);
            }
        }

        errorMsg.forEach(item -> {
            System.out.println(item);
        });

    }

    private String formatLine(String original) {
        char[] lineChars = original.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2) {
                    builder.append(curChar);
                }
            } else {
                builder.append(curChar);
            }
        }
        return builder.toString();
    }

    private String[] buildVPCResult() {
        String[] result = new String[]{
                "Legend:",
                "                (*) - local vPC is down, forwarding via vPC peer-link",
                "",
                "vPC domain id                     : 10  ",
                "Peer status                       : peer adjacency formed ok      ",
                "vPC keep-alive status             : peer is alive                 ",
                "Configuration consistency status  : success ",
                "Per-vlan consistency status       : success                       ",
                "Type-2 consistency status         : success ",
                "vPC role                          : primary                       ",
                "Number of vPCs configured         : 11   ",
                "Peer Gateway                      : Enabled",
                "Peer gateway excluded VLANs       : -",
                "Dual-active excluded VLANs        : -",
                "Graceful Consistency Check        : Enabled",
                "Auto-recovery status              : Enabled (timeout = 240 seconds)",
                "",
                "vPC Peer-link status",
                "---------------------------------------------------------------------",
                "id   Port   Status Active vlans    ",
                "--   ----   ------ --------------------------------------------------",
                "1    Po2    up     1,17,25-26,31,60,71-72,83,88,99,103,110-111,121,13" +
                        "0-131,148-149,177,183-184,186,188,193-194,201-204,206,212,214,216-217,222,243,245," +
                        "248-250,253-254,283,287-288,301-306,340,348,351-353,380-381,599,693",
                "vPC status",
                "----------------------------------------------------------------------",
                "id   Port      Status Consistency Reason                  Active vlans",
                "--   ----      ------ ----------- ------                  ------------",
                "45   Po45      down*  success     success                    -               ",
                "46   Po46      down*  success     success                    -               ",
                "48   Po48      down*  success     success                    -               ",
                "50   Po50      down*  success     success                    1               ",
                "52   Po52      up     success     success                    1,17,25-26,     " +
                "                                                             31,60,71-72     " +
                "                                                             ,83,88,99,1     " +
                "                                                             03,110-111,     " +
                "                                                             121,130-131 ....",
                "110  Po110     down*  success     success                    -               ",
                "249  Po249     up     success     success                    283 "
        };
        return result;
    }

    private String[] buildVPCHistoryResult() {
        String[] result = new String[]{
                "Legend:",
                "                 - local vPC is down, forwarding via vPC peer-link",
                "",
                "vPC domain id                     : 10  ",
                "Peer status                       : peer adjacency formed ok      ",
                "vPC keep-alive status             : peer is alive                 ",
                "Configuration consistency status  : success ",
                "Per-vlan consistency status       : success                       ",
                "Type-2 consistency status         : success ",
                "vPC role                          : primary                       ",
                "Number of vPCs configured         : 9   ",
                "Peer Gateway                      : Enabled",
                "Peer gateway excluded VLANs       : -",
                "Dual-active excluded VLANs        : -",
                "Graceful Consistency Check        : Enabled",
                "Auto-recovery status              : Enabled (timeout = 240 seconds)",
                "",
                "vPC Peer-link status",
                "---------------------------------------------------------------------",
                "id   Port   Status Active vlans    ",
                "--   ----   ------ --------------------------------------------------",
                "1    Po2    up     1,17,25-26,31,60,71-72,83,88,99,103,110-111,121,13" +
                        "0-131,148-149,177,183-184,186,188,193-194,201-204,206,212,214,216-217,222,243,245," +
                        "248-250,253-254,283,287-288,292,301-306,340,348,351-353,380-381,599,693",
                "vPC status",
                "----------------------------------------------------------------------",
                "id   Port      Status Consistency Reason                  Active vlans",
                "--   ----      ------ ----------- ------                  ------------",
                "45   Po45      down*  success     success                    -               ",
                "46   Po46      down*  success     success                    -               ",
                "48   Po48      down*  success     success                    -               ",
                "50   Po50      down*  success     success                    -               ",
                "52   Po52      up     success     success                    1,17,25-26,     " +
                        "                                                             31,60,71-72     " +
                        "                                                             ,83,88,99,1     " +
                        "                                                             03,110-111,     " +
                        "                                                             121,130-131 ....",
                "110  Po110     down*  success     success                    -               ",
                "249  Po249     up     success     success                    283 "
        };
        return result;
    }

    @Test
    public void testReadPortChannelSummaryResult () {
        List<String> errorMsg = new ArrayList<>();
        String[] lines = buildPortChannelSummaryResult();
        boolean beginRead = false;

        int colNum = -1;

        int focusIdx = -1;

        int identifyIdx = -1;

        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*\\/\\d*)\\(([a-zA-Z]*)\\)");

        Map<String, String> oldIndicators = buildHistoryIndicators();

        String errorMsgFormatter = "indicator status change in Port-Channel %s, %s";
        String errorIndicatorFormatter = "interface %s status changed from %s to %s";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("---") && line.endsWith("---")) {
                line = lines[++i];
                List<String> titles = parseProperties(line);
                colNum = titles.size();

                for (int j = 0; j < colNum; j++) {
                    String title = titles.get(j);
                    if (title.equalsIgnoreCase("Port-Channel")) {
                        identifyIdx = j;
                        continue;
                    } else if (title.equalsIgnoreCase("Member Ports")) {
                        focusIdx = j;
                        continue;
                    }
                    if (identifyIdx >= 0 && focusIdx >= 0) {
                        break;
                    }
                }

                for(; !(line.startsWith("---") && line.endsWith("---")) && i < lines.length; line = lines[++i]);
                beginRead = true;
                continue;
            }

            if (!beginRead) {
                continue;
            }

            if (colNum < 0 || focusIdx < 0 || identifyIdx < 0) {
                errorMsg.add("can not find useful info from command execute result");
                break;
            }


            List<String> properties = parseProperties(line, colNum);

//            System.out.println(String.join("|", properties.toArray(new String[properties.size()])));

            String focusVal = properties.get(focusIdx);
            String identifyTitle = properties.get(identifyIdx);



            Matcher matcher = pattern.matcher(focusVal);

            StringBuilder builder = new StringBuilder();
            while (matcher.find()) {
                String interfaceName = matcher.group(1);
                String status = matcher.group(2);
                String oldStatus = oldIndicators.get(interfaceName);
                if (oldStatus == null) {
                    continue;
                }
                if (!oldStatus.equals(status)) {
                    String indicatorErrorString = String.format(errorIndicatorFormatter, interfaceName, oldStatus, status);
                    builder.append(indicatorErrorString);
                    builder.append(",");
                }
            }
            if (builder.length() > 0) {
                String errorText = builder.substring(0, builder.length() - 1);
                String errorItem = String.format(errorMsgFormatter, identifyTitle, errorText);
                errorMsg.add(errorItem);
            }

//            System.out.println(String.join("|", identifyTitle, focusVal));


        }

        for (int i = 0, len = errorMsg.size(); i < len; i++) {
            System.out.println(errorMsg.get(i));
        }
    }

    private List<String> parseProperties(String context, int colNum) {
        List<String> properties = new ArrayList<String>();
        char[] lineChars = context.trim().toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0, curIdx = 1; i < lineChars.length; i++) {
            char curChar = lineChars[i];
//                遇到两个及两个以上的连续的空格或者一个及一个以上的制表符，或两者的组合，则判定是要分隔成单独的字段
            if (curChar == 32 || curChar == 9 || curChar == 0xa0 || curChar == 0xc2) {
                int split = curChar == 9 ? 2 : 0;
                for (; i < lineChars.length && (lineChars[i] == 32 || lineChars[i] == 9 || lineChars[i] == 0xa0
                        || lineChars[i] == 0xc2); i++, split++);
//                    下标要往前退一位（因为为了判断出现第一个非空格和非制表符，多跑了一位）
                i--;
                if (split < 2 || curIdx >= colNum) {
                    builder.append(curChar);
                } else {
                    String property = builder.toString().trim();
                    properties.add(property);
                    builder = new StringBuilder();
                    curIdx++;
                    continue;
                }
            } else {
                builder.append(curChar);
            }
        }
        if (builder.length() > 0) {
            String property = builder.toString().trim();
            properties.add(property);
        }
        return properties;
    }

    private String[] buildPortChannelSummaryResult () {
        String[] result = new String[] {
                "Flags:  D - Down        P - Up in port-channel (members)",
                "        I - Individual  H - Hot-standby (LACP only)",
                "        s - Suspended   r - Module-removed",
                "        S - Switched    R - Routed",
                "        U - Up (port-channel)",
                "        M - Not in use. Min-links not met",
                "--------------------------------------------------------------------------------",
                "Group  Port-Channel       Type                Protocol  Member Ports",
                "--------------------------------------------------------------------------------",
                "1     Po1(SU)     Eth      NONE      Eth1/39(P)   Eth1/40(P)   Eth7/39(P)  Eth7/40(P)",
                "3     Po3(SU)     Eth      NONE      Eth1/24(P)   Eth7/24(P)   ",
                "10    Po10(SU)    Eth      LACP       Eth1/27(P)   Eth7/27(P)   ",
                "91    Po91(SU)    Eth      NONE      Eth1/25(D)   Eth7/25(P)   ",
                "98    Po98(SU)    Eth      NONE      Eth1/1(P)    ",
                "101   Po101(SU)   Eth      NONE      Eth1/33(P)   Eth1/34(P)   Eth7/33(P)	Eth7/34(P)  "
        };
        return result;
    }

    private Map<String, String> buildHistoryIndicators() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("Eth1/39", "P");
        result.put("Eth1/40", "P");
        result.put("Eth7/39", "P");
        result.put("Eth7/40", "P");
        result.put("Eth1/24", "P");
        result.put("Eth7/24", "S");
        result.put("Eth1/27", "r");
        result.put("Eth7/27", "P");
        result.put("Eth1/25", "D");
        result.put("Eth7/25", "P");
        result.put("Eth1/1", "P");
        result.put("Eth1/33", "P");
        result.put("Eth1/34", "P");
        result.put("Eth7/33", "P");
        result.put("Eth7/34", "P");
        return result;
    }

}
