package com.gwg.demo.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HelloReceiver {
	
	private static final Logger logger = LoggerFactory.getLogger(HelloReceiver.class);
	 
    public void consumeMessage(String hello) {
    	logger.info("Receiver  : " + hello);
    }
 
}
