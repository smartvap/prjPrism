package org.ayakaji.verify;

import java.io.File;

public class PathVerifier {

	public static void main(String[] args) {
		File f = new File("cisco.ini");
		System.out.println(f.exists());
	}

}
