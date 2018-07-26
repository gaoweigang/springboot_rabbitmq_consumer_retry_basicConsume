package com.gwg.demo.mq.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import com.gwg.demo.mq.consumer.process.MessageProcess;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;

public class MessageConsumer{

	private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
	
	private String exchange;
	private String queue;
	private String routing;
	
    private MessageProcess messageProcess;
	
	private ConnectionFactory connectionFactory;
	
	public MessageConsumer(ConnectionFactory connectionFactory, MessageProcess messageProcess, String exchange, String queue, String routing){
		logger.info("exchange:{}, queue:{}, routing:{}", exchange, queue, routing);
		this.connectionFactory = connectionFactory;
		this.messageProcess = messageProcess;
		this.exchange = exchange;
		this.queue = queue;
		this.routing = routing;
	}
    
	/**
	 * 这个使用的消息队列监听器，需要队列提前创建好，否则会报错
	 */
	public void consume() throws IOException {
		logger.info("使用推模式消费消息，线程id:{}", Thread.currentThread().getId());
        this.buildMessageConsumer();
	}

	/**********consumer 
	 * @param <T>
	 * @param <T>*******************************************************************************************/
	//编写注解 启动RabbitMQ消费， @RabbitmqConsumer来解决这个问题
	public <T> void buildMessageConsumer() throws IOException{
		logger.info("consumer exchange:{}, queue:{}, routing:{}", exchange, queue, routing);
		Connection connection = connectionFactory.createConnection();
		//不支持事物
	    Channel channel = connection.createChannel(false);
		//消费者尝试构建exchange, queue, routing
		channel.exchangeDeclare(exchange, "direct", true, false, null);
		channel.queueDeclare(queue, true, false, false, null);
		channel.queueBind(queue, exchange, routing);

		Consumer consumer = new CustomConsumer<T>(channel, connection, messageProcess);

		//设置为手动确认
		channel.basicConsume(queue, false, consumer);
		
	}


}
