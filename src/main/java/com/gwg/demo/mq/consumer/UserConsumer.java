package com.gwg.demo.mq.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers.Default;
import com.gwg.demo.mq.common.Action;
import com.gwg.demo.mq.common.DetailResult;
import com.gwg.demo.mq.common.MessageConsumer;
import com.gwg.demo.mq.consumer.message.process.MessageProcess;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

@Component
public class UserConsumer{

	private static final Logger logger = LoggerFactory.getLogger(UserConsumer.class);
	
	@Value("${rabbitmq.direct.exchange}")
	private String exchange;
	@Value("${rabbitmq.queue}")
	private String queue;
	@Value("${rabbitmq.routing}")
	private String routing;
	
	@Autowired
	@Qualifier("userMessageProccess")
    private MessageProcess messageProcess;
	
	@Autowired
	private ConnectionFactory connectionFactory;
    
	/**
	 * 这个使用的消息队列监听器，需要队列提前创建好，否则会报错
	 */
	@RabbitListener(queues = "${rabbitmq.queue}")
	public void consume() throws IOException {
		logger.info("尝试从队列获取50条消息 start ....."+Thread.currentThread().getId());
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
		
		//设置Message的序列化方法
		final MessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();
		final MessageConverter messageConverter = new Jackson2JsonMessageConverter();
		
		
		Consumer consumer = new CustomConsumer<T>(channel, connection, messageProcess);

		//设置为手动确认
		channel.basicConsume(queue, false, consumer);
		
	}


}
