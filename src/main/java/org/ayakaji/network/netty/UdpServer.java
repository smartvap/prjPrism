package org.ayakaji.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.ChannelPipeline;

import java.net.InetAddress;

/**
 * Discards any incoming data.
 */
public class UdpServer {

	private int port;

	public UdpServer(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		final NioEventLoopGroup group = new NioEventLoopGroup();
		try {
			final Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.handler(new ChannelInitializer<NioDatagramChannel>() {
						@Override
						public void initChannel(final NioDatagramChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							p.addLast(new IncomingPacketHandler());
						}
					});

			// Bind and start to accept incoming connections.
			InetAddress address = InetAddress.getLocalHost();
			b.bind(address, port).sync().channel().closeFuture().await();

		} finally {
			System.out.print("In Server Finally");
		}
	}

	public static void main(String[] args) throws Exception {
		final int port = 9956;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new UdpServer(port).run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

	}
}