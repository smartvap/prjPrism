package org.ayakaji.verify;

import java.util.Timer;
import java.util.TimerTask;

public class TaskVerifier {

	public static void main(String[] args) {
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Hello!");
			}
		}, 5000);
		t.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				System.out.println("Hello!");
			}
		}, 5000, 3000);
	}

}
