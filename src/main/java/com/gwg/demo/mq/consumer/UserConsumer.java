package com.gwg.demo.mq.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gwg.demo.mq.common.MessageConsumer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ShutdownSignalException;

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
	@RabbitListener(queues = "${rabbitmq.queue}")
	public void consume() throws IOException {
		logger.info("消息消费 start .....");
		messageConsumer.consume();
		
	}
 
}
