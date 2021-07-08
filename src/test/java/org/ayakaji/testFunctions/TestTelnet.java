package org.ayakaji.testFunctions;

import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestTelnet {

    @Test
    public void testTelnetConnect() throws IOException {
        TelnetClient client = new TelnetClient("ansi");
        client.setConnectTimeout(5000);
        client.connect("192.168.30.132", 23);
        /*send(client,"^]");
        getReceivedMsg(client).forEach(item -> {
            System.out.println(item);
        });
        send(client, "ayt");
        */

        /*getReceivedMsg(client).forEach(item -> {
            System.out.println(item);
        });*/
        String[] msg = readLines(client, "login:");
        Arrays.stream(msg).forEach(item->{
            System.out.println("returnMsg:" + item);
        });
        send(client, "zhangdatong");
        msg = readLines(client, "Password:");
        Arrays.stream(msg).forEach(item->{
            System.out.println("returnMsg:" + item);
        });
        send(client, "!QAZ2wsx");
        msg = readLines(client, "$");
        Arrays.stream(msg).forEach(item->{
            System.out.println("returnMsg:" + item);
        });
        send(client, "cd /");
        msg = readLines(client, "$");
        Arrays.stream(msg).forEach(item->{
            System.out.println("returnMsg:" + item);
        });
        send(client, "ll");
        msg = readLines(client, "$");
        Arrays.stream(msg).forEach(item->{
            System.out.println("returnMsg:" + item);
        });
        client.disconnect();
    }

    private List<String> getReceivedMsg(TelnetClient client) throws IOException {
        List<String> msg = new ArrayList<String>();
        InputStream inputStream = client.getInputStream(); //读取命令的流
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader bReader = new BufferedReader(reader);
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1024];
        for (int size = reader.read(buf); size > 0; size = reader.read(buf)) {
            String item = new String(Arrays.copyOf(buf, size));
            System.out.println(item);
            msg.add(item);
        }
        return msg;
    }



    private String getReceiveMsgUntil(TelnetClient client, String expectedMsg) throws IOException {
        String resultMsg = "msg:";
        InputStream inputStream = client.getInputStream(); //读取命令的流
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        /*BufferedReader bReader = new BufferedReader(reader);*/
        char[] buf = new char[1024];
        for (int size = reader.read(buf); size > 0; size = reader.read(buf)) {
            String item = new String(Arrays.copyOf(buf, size));
            System.out.println(item);
            resultMsg += item;
            if (item.trim().endsWith(expectedMsg)) {
                break;
            }
        }

        return resultMsg;
    }

    private String[] readLines(TelnetClient client, String expectedMsg) throws IOException {
        String[] lines = null;
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = client.getInputStream(); //读取命令的流
        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
        char[] buf = new char[100];
        for (int size = reader.read(buf); size > 0; size = reader.read(buf)) {
            String item = new String(Arrays.copyOf(buf, size));
            builder.append(item);
            if (item.trim().endsWith(expectedMsg)) {
                break;
            }
        }
        lines = builder.toString().split("\r\n");
        lines = Arrays.copyOf(lines, lines.length - 1);
        return lines;
    }

    private void send(TelnetClient client, String command) {
        OutputStream os = client.getOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.print(command);
        ps.print("\r\n");
        ps.flush();
    }

}
