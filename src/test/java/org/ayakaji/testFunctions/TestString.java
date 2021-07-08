package org.ayakaji.testFunctions;

import org.ayakaji.pojo.AlarmLevel;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestString {
    @Test
    public void testStringFormat() {
        String cmd = "ds :: ";
        String argument = "arg1";
        String result = String.format(cmd + "[%s]", argument);
        System.out.println(result);
    }

    @Test
    public void testLevelStr() {
        AlarmLevel level = AlarmLevel.URGENT;
        System.out.println(level.ordinal());
        Arrays.stream(AlarmLevel.values()).forEach(lvl-> {
            System.out.println(lvl.ordinal());
        });
    }

    @Test
    public void testCiscoNeighborsResultAnalysis() {
        String[] model = new String[12];
        model[0] = "JS-DC01-N7K-1-Access# show cdp neighbors ";
        model[1] = "Capability Codes: R - Router, T - Trans-Bridge, B - Source-Route-Bridge";
        model[2] = "                  S - Switch, H - Host, I - IGMP, r - Repeater,";
        model[3] = "                  V - VoIP-Phone, D - Remotely-Managed-Device,";
        model[4] = "                  s - Supports-STP-Dispute";
        model[5] = "";
        model[6] = "Device-ID          Local Intrfce  Hldtme Capability  Platform      Port ID";
        model[7] = "GQT-DC02-N7K-1-Access(JAF1638AABD)  Eth1/1         131    R S I s   N7K-C7010     Eth1/1 ";
        model[8] = "JS-DC01-C4507R-JingFen  Eth1/24        158    R S I     WS-C4507R-E   Gig1/23 ";
        model[9] = "JS-DC01-C6509-1-Kernel.sdboss.com   Eth1/28        136    R S I     WS-C6509-E    Ten7/2 ";
        model[10] = "JS-DC01-N7K-2-Access(JAF1811AGEE)  Eth1/39        131    R S I s   N7K-C7010     Eth1/39";
        model[11] = "JS-DC01-N7K-2-Access(JAF1811AGEE)	Eth1/40        131    R S I s   N7K-C7010     Eth1/40";
        String titleFlag = "Device-ID";
        boolean beginLoad = false;
        for (int i = 0; i < model.length; i++) {
            String result = model[i];
            if (result.startsWith(titleFlag)) {
                beginLoad = true;
            }
            if (beginLoad) {
                char[] eles = result.toCharArray();
                StringBuilder builder = new StringBuilder();
                for (int j = 0; j < eles.length; j++) {
                    char item = eles[j];
                    if (eles[j] == '\t') {
                        for (; j < eles.length && (eles[j] == 32 || eles[j] == 0xc2 || eles[j] == 0xa0 || eles[j] == '\t'); j++);
                        builder.append("|");
                        if (j < eles.length && !(eles[j] == 32 || eles[j] == 0xa0 || eles[j] == 0xc2 || eles[j] == 9)) {
                            builder.append(eles[j]);
                        }
                        continue;
                    }
                    if (eles[j] == 0xC2 || eles[j] == 0xA0 || eles[j] ==32 || eles[j] == 9) {
                        int spaceCnt = 0;
                        for (; j < eles.length && (eles[j] == 32 || eles[j] == 0xa0 || eles[j] == 0xc2 || eles[j] == 9); j++, spaceCnt++);
                        if (spaceCnt > 1) {
                            builder.append("|");
                        } else {
                            builder.append(item);
                        }
                    }
                    if (j < eles.length && !(eles[j] == 32 || eles[j] == 0xa0 || eles[j] == 0xc2 || eles[j] == 9)) {
                        builder.append(eles[j]);
                    }
                }
                System.out.println(builder.toString());
            }
        }

    }

    @Test
    public void testSpace() {
        String testStr = " ";
        char[] eles = testStr.toCharArray();
        char ele = eles[0];
        System.out.println((byte)ele);
        System.out.println(ele == 32);
        System.out.println(ele == 0xc2);
        System.out.println(ele == 0xa0);
    }

    @Test
    public void testRemoveSpaceFlag() {
        String testStr = "JS-DC01-N7K-2-Access(JAF1811AGEE)	Eth1/40        131    R S I s   N7K-C7010     Eth1/40";
        StringBuilder builder = new StringBuilder();
        char[] testArr = testStr.toCharArray();
        for (int i = 0; i < testArr.length; i++) {
            char item = testArr[i];
            if (testArr[i] == '\t') {
                for (; i < testArr.length && (testArr[i] == 32 || testArr[i] == 0xc2 || testArr[i] == 0xa0 || testArr[i] == '\t'); i++);
                builder.append("|");
                if (i < testArr.length) {
                    builder.append(testArr[i]);
                }
                continue;
            }
            if (testArr[i] == 32 || testArr[i] == 0xc2 || testArr[i] == 0xa0) {
                int spaceCnt = 0;
                for (; i < testArr.length && (testArr[i] == 32 || testArr[i] == 0xc2 || testArr[i] == 0xa0 || testArr[i] == '\t'); i++, spaceCnt++);
                if (spaceCnt > 1) {
                    builder.append("|");
                } else {
                    builder.append(item);
                }

            }
            if (i < testArr.length) {
                builder.append(testArr[i]);
            }
        }
        System.out.println(builder.toString());
    }

    @Test
    public void testSpecialSpace() {
        String str = "\t";
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            System.out.println(chars[i]);
        }
    }


    @Test
    public void testAnalyzeShowVersionResult() {
        String [] resultArr = buildShowVersionResult();
        String updateTimeRegex = "Kernel uptime is (\\d*) day\\(s\\), (\\d*) hour\\(s\\), (\\d*) minute\\(s\\), (\\d*) second\\(s\\)";
        Pattern pattern = Pattern.compile(updateTimeRegex);
//        System.out.println(String.join("\t", "days", "hours", "minutes", "seconds"));
        for (int i = 0; i < resultArr.length; i++) {
            String line = resultArr[i];
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String days = matcher.group(1);
                String hours = matcher.group(2);
                String minues = matcher.group(3);
                String seconds = matcher.group(4);
//                System.out.println(String.join("\t", days, hours, minues, seconds));
                System.out.println(String.format("%s %s:%s:%s", days, hours, minues, seconds));
                break;
            }
        }
    }

    private String[] buildShowVersionResult() {
        List<String> builderList = new ArrayList<String>(32);
        builderList.add("Show module");
        builderList.add("show redundancy status");
        builderList.add("Show module");
        builderList.add("Cisco Nexus Operating System (NX-OS) Software");
        builderList.add("TAC support: http://www.cisco.com/tac");
        builderList.add("Documents: http://www.cisco.com/en/US/products/ps9372/tsd_products_support_series_home.html");
        builderList.add("Copyright (c) 2002-2016, Cisco Systems, Inc. All rights reserved.");
        builderList.add("The copyrights to certain works contained in this software are");
        builderList.add("owned by other third parties and used and distributed under");
        builderList.add("license. Certain components of this software are licensed under");
        builderList.add("the GNU General Public License (GPL) version 2.0 or the GNU");
        builderList.add("Lesser General Public License (LGPL) Version 2.1. A copy of each");
        builderList.add("such license is available at");
        builderList.add("http://www.opensource.org/licenses/gpl-2.0.php and");
        builderList.add("http://www.opensource.org/licenses/lgpl-2.1.php");
        builderList.add("");
        builderList.add("Software");
        builderList.add("  BIOS:      version N/A");
        builderList.add("  kickstart: version 6.2(16)");
        builderList.add("  system:    version 6.2(16)");
        builderList.add("  BIOS compile time:       ");
        builderList.add("  kickstart image file is: bootflash:///n7000-s2-kickstart.6.2.16.bin");
        builderList.add("  kickstart compile time:  1/27/2016 9:00:00 [04/06/2016 05:13:37]");
        builderList.add("  system image file is:    bootflash:///n7000-s2-dk9.6.2.16.bin");
        builderList.add("  system compile time:     1/27/2016 9:00:00 [04/06/2016 06:16:06]");
        builderList.add("");
        builderList.add("");
        builderList.add("Hardware");
        builderList.add("  cisco Nexus7000 C7010 (10 Slot) Chassis (\"Supervisor Module-2\")");
        builderList.add("  Intel(R) Xeon(R) CPU         with 32745060 kB of memory.");
        builderList.add("  Processor Board ID JAF1810AHFN");
        builderList.add("");
        builderList.add("  Device name: JS-DC01-N7K-1-Access");
        builderList.add("  bootflash:    1966080 kB");
        builderList.add("  slot0:              0 kB (expansion flash)");
        builderList.add("");
        builderList.add("Kernel uptime is 461 day(s), 23 hour(s), 53 minute(s), 51 second(s)");
        builderList.add("");
        builderList.add("Last reset ");
        builderList.add("  Reason: Unknown");
        builderList.add("  System version: 6.2(16)");
        builderList.add("  Service: ");
        builderList.add("");
        builderList.add("plugin");
        builderList.add("  Core Plugin, Ethernet Plugin");
        String[] arr = builderList.toArray(new String[32]);
        return arr;
    }




    /**
     * 解析结果文本中的指标信息
     * @param context 整行的命令结果信息
     * @return 有效信息的属性数组
     */
    private String[] parseProperties (String context) {
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
        String[] cols = properties.toArray(new String[properties.size()]);
        return cols;
    }

    @Test
    public void testUpTime() {
        String[] lines = new String[] {
                "System uptime:              0 days, 23 hours, 47 minutes, 37 seconds",
                "Kernel uptime:              461 days, 23 hours, 53 minutes, 51 seconds",
                "Active supervisor uptime:   461 days, 23 hours, 47 minutes, 37 seconds"
        };
        Pattern pattern = Pattern.compile("[\\s\\S]*0[^\\d]*day[\\s\\S]*");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                System.out.println(true);
            } else {
                System.out.println(false);
            }
        }
    }

    @Test
    public void testSplit() {
        String testStr = "Supply    Model                    Output     Capacity    Status";
        String[] strArr = testStr.split(" ");
        System.out.println(Arrays.deepToString(strArr));
    }

    @Test
    public void testFindWithRegex() {
        String example = "       ------ Module 1 -----            ";
        Pattern pattern = Pattern.compile("[ \\f\\r\\t\\n]*[\\-]*([^\\-]*)[\\-]*");
        Matcher matcher = pattern.matcher(example);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }

    @Test
    public void testFindIndicator() {
        String example = "Up Time:             461 days, 23 hours, 48 minutes, 18 seconds";
        Pattern indicatorPattern = Pattern.compile("Up Time:[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*day[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*hour[s]?,[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*minute[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*second[s]?");
        Matcher matcher = indicatorPattern.matcher(example);
        if (matcher.find()) {
            String format = "%s %s:%s:%s";
            System.out.println(String.format(format, matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
        }
    }

    @Test
    public void testNumFind () {
        String example = "Up Time:             461 days, 23 hours, 48 minutes, 18 seconds";
        Pattern pattern = Pattern.compile("Up Time:[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*day[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*hour[s]?,[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*minute[s]?," +
                "[ \\f\\r\\t\\n]*([\\d]*)[ \\f\\r\\t\\n]*second[s]?");
        Matcher matcher = pattern.matcher(example);
        System.out.println(matcher.find());
    }

    @Test
    public void testMathrocessesCpuLine () {
        String example = "CPU utilization for five seconds: 1%/0%; one minute: 1%; five minutes: 1%";
        Pattern indicatorPattern = Pattern.compile("CPU utilization for five seconds: ([\\d]*)%/([\\d]*)%; " +
                "one minute: ([\\d]*)%; five minutes: ([\\d]*)%");
        Matcher matcher = indicatorPattern.matcher(example);
        if (matcher.find()) {
            System.out.println(String.join("\t", matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
        }
    }

    @Test
    public void testCPUState() {
        String example = "CPU states  :   0.42% user,   0.42% kernel,   99.15% idle";
        Pattern pattern = Pattern.compile("([\\d]*(.[\\d]*)?)% idle");
        Matcher matcher = pattern.matcher(example);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        }
    }

    @Test
    public void testMemUsage () {
        String example = "Memory usage:   32745060K total,   5555916K used,   27189144K free";
        Pattern pattern = Pattern.compile("Memory usage:[ \\t\\r\\n\\f]*([\\d]*)K total, [ \\t\\r\\n\\f]*([\\d]*)K used");
        Matcher matcher = pattern.matcher(example);
        if (matcher.find()) {
            System.out.println(String.join(" : ", matcher.group(2), matcher.group(1)));
        }
    }

    @Test
    public void  testErrLog() {
        String example = "2021 May 20 10:24:05 SC-DC06-N7718-01-BOSS-C10 %ETHPORT-4-IF_XCVR_WARNING: Interface Ethernet8/9, Low Rx Power Warning";
        Pattern logLevelPattern = Pattern.compile("%[^-]*-([\\d]{1})-");
        Matcher matcher = logLevelPattern.matcher(example);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group());
        }
    }

    @Test
    public void testReadAndDeal() throws IOException {
        Path path = Paths.get("H:\\测试数据\\原始用户数据\\VSS订购\\alldinggou_vss_20210127.txt");
        List<String> lines = Files.lines(path).map(item ->item.replaceAll("\t", "|")
                .trim()).sorted().collect(Collectors.toList());

    }

    @Test
    public void testStringReplaceAction() {
        String source = this.getClass().getName();
        String classPath = source.replace(".", "/");
        System.out.println(classPath);
    }

    @Test
    public void testStringOfNull() {
        String nullString = "" + null;
        System.out.println(nullString);
        System.out.println(nullString.substring(2));
    }
}
