package org.ayakaji;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ayakaji.scheduling.ScheduleDeamon;

public class Application {

    private transient static Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("prism begin to start");
        ScheduleDeamon.getInstance().start();
        logger.info("prism started");
    }
}
