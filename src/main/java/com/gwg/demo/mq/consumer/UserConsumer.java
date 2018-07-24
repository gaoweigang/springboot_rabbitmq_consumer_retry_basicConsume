package com.gwg.demo.mq.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gwg.demo.mq.common.MessageConsumer;

@Component
public class UserConsumer {
	
	private static final Logger logger = LoggerFactory.getLogger(UserConsumer.class);
	
	@Value("${rabbitmq.direct.exchange}")
	private String exchange;

	@Value("${rabbitmq.queue}")
	private String queue;
	
	@Value("${rabbitmq.routing}")
	private String routing;
	
	@Autowired
	private MessageConsumer messageConsumer;
	 
	/**
	 * 这个使用的消息队列监听器，需要队列提前创建好，否则会报错
	 */
	public void consume() throws IOException {
		logger.info("尝试从队列获取50条消息 start .....");
		for(int i = 0; i < 50; i++){
			messageConsumer.consume();
		}

	}
 
}
