package com.gwg.demo.mq.config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.fastjson.JSON;
import com.gwg.demo.mq.common.Constant;
import com.gwg.demo.mq.common.DetailResult;
import com.gwg.demo.mq.common.MessageConsumer;
import com.gwg.demo.mq.common.MessageProducer;
import com.gwg.demo.mq.consumer.message.process.MessageProcess;
import com.gwg.demo.mq.consumer.message.process.impl.UserMessageProcess;
import com.gwg.demo.mq.message.UserMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;

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
	
	/**********consumer *******************************************************************************************/
	@Bean
	public <T> MessageConsumer buildMessageConsumer() throws IOException{
		logger.info("consumer exchange:{}, queue:{}, routing:{}", exchange, queue, routing);
		Connection connection = connectionFactory().createConnection();
		//不支持事物
		final Channel channel = connection.createChannel(false);
		//消费者尝试构建exchange, queue, routing
		channel.exchangeDeclare(exchange, "direct", true, false, null);
		channel.queueDeclare(queue, true, false, false, null);
		channel.queueBind(queue, exchange, routing);
		
		//设置Message的序列化方法
		final MessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();
		final MessageConverter messageConverter = new Jackson2JsonMessageConverter();
		
		return new MessageConsumer() {
			
			@Override
			public DetailResult consume() {
				try {
					GetResponse response = channel.basicGet(queue, false);	
					while(response == null){
						response = channel.basicGet(queue, false);
					    Thread.sleep(Constant.ONE_SECOND);
					}
					logger.info("从队列中获取的消息：{}",JSON.toJSON(response));
					Message message = new Message(response.getBody(), messagePropertiesConverter
							.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));
					T messageBean = (T) messageConverter.fromMessage(message);
					logger.info("consume 消息处理 start....，消息内容：", JSON.toJSON(messageBean));
					DetailResult result = userMessageProccess().process(messageBean);
					//手动确认
					if(result.isSuccess()){//
						logger.info("消费成功 返回确认消息....");
						channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
					}else{
						logger.info("消费失败,消息重新入队....");
						channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);//前提是需要设置消息确认模式为手动，否则无效
					}
					return result;
				} catch (IOException e) {
					e.printStackTrace();
					return new DetailResult(false, e.getMessage());
				} catch (InterruptedException e) {
					e.printStackTrace();
					return new DetailResult(false, e.getMessage());
				} finally{
					try {
						channel.close();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (TimeoutException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	//用户消息处理类
	@Bean
	public <T> UserMessageProcess userMessageProccess(){
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
	
	/******************message listener************************************************************/
	/**
	 * 监听器配置
	 */
	@Bean
	public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(){
		SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
		rabbitListenerContainerFactory.setConnectionFactory(connectionFactory());
		rabbitListenerContainerFactory.setConcurrentConsumers(1);
		rabbitListenerContainerFactory.setMaxConcurrentConsumers(10);
		rabbitListenerContainerFactory.setAcknowledgeMode(AcknowledgeMode.MANUAL);//设置消费者消息确认模式为手动
		return rabbitListenerContainerFactory;
	}


}
