package org.ayakaji.cisco.analyzers;

import org.ayakaji.cisco.analyzers.anocation.AnalyzerName;
import org.ayakaji.cisco.pojo.DetectResult;
import org.ayakaji.pojo.AlarmLevel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * show ip ospf neighbors命令执行结果解析器
 * @author zhangdatong
 * @date 2021/06/16 17:54
 * @version 1.0.0
 */
@AnalyzerName("SHOW_IP_OSPF_NEIGHBORS")
public class IPOspfNeighborsAnalyzer extends ResultAnalyzer {

    public IPOspfNeighborsAnalyzer(String hostName, String command, String argument, Object detectResult,
                                   Date detectTime) {
        super(hostName, command, argument, detectResult, detectTime);
    }

    /**
     * show ip ospf neighbors命令返回的结果解析，检查Uptime属性，发现Uptime时长在一天以内的ospf需要记录IP地址并返回告警
     * ywdh分别代表年、周、天、小时，需要排查不到一天以内的ospf收敛，如0d1h，代表不到一天，一小时之前。
     * 结果示例：
     * OSPF Process ID 10 VRF default
     *  Total number of neighbors: 9
     *  Neighbor ID     Pri State            Up Time  Address         Interface
     *  10.19.93.249      1 FULL/BDR         1y13w    10.19.93.204    Vlan98
     *  10.19.93.250      1 FULL/DR          1y13w    10.19.93.203    Vlan98
     *  10.19.93.253      1 TWOWAY/DROTHER   1y13w    10.19.93.202    Vlan98
     *  10.19.93.245      1 FULL/DR          1y13w    10.19.93.156    Vlan94
     *  10.19.93.246      1 FULL/DROTHER     28w2d    10.19.93.155    Vlan94
     *  10.19.93.253      1 FULL/DROTHER     28w2d    10.19.93.154    Vlan94
     *  10.19.93.251      1 FULL/DROTHER     1y13w    10.19.93.213    Vlan2
     *  10.19.93.252      1 FULL/DROTHER     1y13w    10.19.93.212    Vlan2
     *  10.19.93.253      1 FULL/BDR         1y13w    10.19.93.209    Vlan2
     * @return  告警信息集合
     * @throws Exception
     */
    @Override
    public List<String> analysisResult() throws Exception {
        List<String> errorMsg = new ArrayList<>();

        DetectResult resultInfo = new DetectResult(hostName, command, argument, detectTime);
        String[] lines = (String[]) detectResult;
        String resultDetail = buildDetailResult(lines);
        resultInfo.setDetailResult(resultDetail);

        int addrIdx = -1;
        int upTimeIdx = -1;
        Pattern beginReadPattern = Pattern.compile("Total number of neighbors:[ ]*([\\d]*)");
        String errorMsgFormatter = "%s:%s";
        for (int i = 0, curRow = -2, rowNum = -1; i < lines.length && curRow < rowNum; i++) {
            String line = lines[i];
            if (line.contains("Total number of neighbors")) {
//                如果符合条件，则正式开始读取
                Matcher matcher = beginReadPattern.matcher(line);
                if (matcher.find()) {
                    String rowNums = matcher.group(1);
                    rowNum = Integer.valueOf(rowNums).intValue();
                    curRow = 0;
                }
                String titleLine = lines[++i];
                int[] forcusIdxs = analyzeTitle(titleLine);
                addrIdx = forcusIdxs[0];
                upTimeIdx = forcusIdxs[1];
                continue;
            }
            if (curRow >= 0) {
                List<String> elements = parseProperties(line);
                String upTime = elements.get(upTimeIdx);
                String address = elements.get(addrIdx);
                resultInfo.addIndicator(address, upTime);
                if (upTime.startsWith("0d")) {
                    errorMsg.add(String.format(errorMsgFormatter, address, upTime));
                }
            }
        }

        if (resultInfo.getIndicators().isEmpty()) {
            errorMsg.add("命令show ip ospf neighbors返回结果解析异常，未找到指标信息");
            resultInfo.addIndicator("Exception", "no result");
        } else {
            errorMsg.add(0, "命令show ip ospf neighbors执行异常");
        }

        if (errorMsg.isEmpty()) {
            resultInfo.setLevel(AlarmLevel.INFO);
        } else {
            resultInfo.setLevel(AlarmLevel.ALARMING);
        }

        sendDetectResult(resultInfo);

        return errorMsg;
    }

    /**
     * 从标题行中解析我们关注的列的序号，返回一个序号的数组。IP地址列号为数组第0位，Uptime列序号为数组第1位
     * @param titleLine 标题行
     * @return 关注的列序号数组
     */
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
}
