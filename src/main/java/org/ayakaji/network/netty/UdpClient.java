package org.ayakaji.network.netty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClient {
	public static void send(String content) {
		send("134.80.19.88", 9514, content);
	}

	public static void send(String ip, int port, String content) {
		byte[] buf = new byte[256];
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		InetAddress address = null;
		try {
			address = InetAddress.getByName(ip);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		try {
			buf = content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		try {
			socket.send(packet);
			System.out.println("发送:" + new String(buf));
		} catch (IOException e) {
			e.printStackTrace();
		}
		packet = null;
		socket.close();
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			UdpClient.send("134.80.19.88", 9956,
					"" + i + ": " + "{\"command\":\"heartbeat_resp\",\"status\":\"一切正常\"}");
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}