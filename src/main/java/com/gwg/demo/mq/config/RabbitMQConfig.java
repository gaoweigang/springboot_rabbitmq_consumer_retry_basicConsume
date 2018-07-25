package com.gwg.demo.mq.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gwg.demo.mq.common.DetailResult;
import com.gwg.demo.mq.common.MessageProducer;
import com.gwg.demo.mq.consumer.message.process.MessageProcess;
import com.gwg.demo.mq.consumer.message.process.impl.UserMessageProcess;
import com.rabbitmq.client.Channel;

/**
 * 
 */
@Configuration
public class RabbitMQConfig {

	private static Logger logger = LoggerFactory
			.getLogger(RabbitMQConfig.class);

	// 测试 调试环境
	@Value("${rabbitmq.host}")
	private String host;
	@Value("${rabbitmq.username}")
	private String username;
	@Value("${rabbitmq.password}")
	private String password;
	@Value("${rabbitmq.port}")
	private Integer port;
	@Value("${rabbitmq.virtual-host}")
	private String virtualHost;// 虚拟主机
	
	@Value("${rabbitmq.direct.exchange}")
	private String exchange;

	@Value("${rabbitmq.queue}")
	private String queue;
	@Value("${rabbitmq.routing}")
	private String routing;
	

	@Bean
	public ConnectionFactory connectionFactory() {
		logger.info("用户名：{}， 密码：{}， 端口号：{}， 虚拟主机：{}", username, password, port, virtualHost);
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
		connectionFactory.setUsername(username);
		connectionFactory.setPassword(password);
		connectionFactory.setPort(port);
		connectionFactory.setVirtualHost(virtualHost);
		return connectionFactory;
	}
	
	
	//用户消息处理类
	@Bean("userMessageProccess")
	public <T> MessageProcess userMessageProccess(){
		return new UserMessageProcess<T>();
	}
	
	
	/**********producer *************************************************************************************************/
	@Bean
	public MessageProducer messageProducer() throws IOException{
		logger.info("producer exchange:{}, queue:{}, routing:{}", exchange, queue, routing);
		Connection connection = connectionFactory().createConnection();
		
		//不支持事物
		final Channel channel = connection.createChannel(false);
		//消费者尝试构建exchange, queue, routing
		channel.exchangeDeclare(exchange, "direct", true, false, null);
		channel.queueDeclare(queue, true, false, false, null);
		channel.queueBind(queue, exchange, routing);
		
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
		rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
		
		return new MessageProducer() {
			@Override
			public DetailResult produce(Object message) {
				try {
					logger.info("发送消息：{}", message);
					rabbitTemplate.convertAndSend(exchange, routing, message);
				} catch (AmqpException e) {
					logger.info("将消息丢入到mq队列失败:{}", e.getMessage());
					return new DetailResult(false, e.getMessage());
				}
				return new DetailResult(true, "");
			}
		};
	}

}
