package org.ayakaji.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetAddress;

public class IncomingPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	IncomingPacketHandler() {
	}

	@Override
	protected void channelRead0(ChannelHandlerContext arg0, DatagramPacket packet) throws Exception {
		final InetAddress srcAddr = packet.sender().getAddress();
		final int port = packet.sender().getPort();
		final ByteBuf buf = packet.content();
		final int rcvPktLength = buf.readableBytes();
		final byte[] rcvPktBuf = new byte[rcvPktLength];
		buf.readBytes(rcvPktBuf);
		System.out.println("从" + srcAddr.toString() + ":" + port + "收到数据:" + new String(rcvPktBuf));
	}
}