package org.ayakaji.network.prism;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestSystem {

    @Test
    public void testGetHostName() throws UnknownHostException {
        String hostName = InetAddress.getLocalHost().getCanonicalHostName();
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        System.out.println(hostName);
    }
}
