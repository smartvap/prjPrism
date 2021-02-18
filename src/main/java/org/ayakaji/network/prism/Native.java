package org.ayakaji.network.prism;
/**
 * Pls run this command in 'd:\workspaces\prjPrism\src\main\java' to generate jni headers: 
 *  javah -d d:\workspaces\prjPrism\src\main\jni org.ayakaji.network.prism.Native
 * @author heqiming
 *
 */
public class Native {
	static native void say(String src);

	static {
		System.loadLibrary("libname");
	}
}