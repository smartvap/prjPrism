package org.ayakaji.verify;

import java.util.Timer;
import java.util.TimerTask;

public class TaskVerifier {

	public static void main(String[] args) throws InterruptedException {
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Hello Once!");
			}
		}, 2000);
		t.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				System.out.println("Hello Cycle!");
			}
		}, 1000, 3000);
		Thread.sleep(10000);
		t.cancel();
		t.purge();
		t = null;
	}

}
