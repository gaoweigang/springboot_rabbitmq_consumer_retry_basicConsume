package com.gwg.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;


/**
 * 
 *在这里我在rabbitmq服务器上已经使用命令创建了交换器，队列，以及交换器与队列的绑定关系
 *因此就不在需要AmqpAdmin，Exchange,Queue,Binding了
 */
@Configuration
public class RabbitMQConfig {

	private static Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

	// 测试 调试环境
	@Value("${rabbitmq.host}")
	private String host;
	@Value("${rabbitmq.username}")
	private String username;
	@Value("${rabbitmq.password}")
	private String password;
	@Value("${rabbitmq.port}")
	private Integer port;

	@Value("${rabbitmq.direct.exchange}")
	private String exchangeName;
	
	@Value("${rabbitmq.queue}")
	private String queueName;// 同时作为rountingkey
	
	@Value("${rabbitmq.virtual-host}")
	private String virtualHost;// 虚拟主机
	
	
	@Bean
	public ConnectionFactory connectionFactory() {
		logger.info("用户名：{}， 密码：{}， 端口号：{}， 虚拟主机：{}", username, password, port, virtualHost);
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
		connectionFactory.setUsername(username);
		connectionFactory.setPassword(password);
		connectionFactory.setPort(port);
		connectionFactory.setVirtualHost(virtualHost);
		connectionFactory.setPublisherReturns(true);
		return connectionFactory;
	}

    /** rabbit:admin用于管理（创建和删除） exchanges, queues and bindings等  **/
	/*@Bean
	public AmqpAdmin mqAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}*/

	
	
	/**
	 * next 必须要生成bean，否则如果不会自动生成该EXCHANGE
	 * 1.生成exchange
	 * 2.生成队列
	 * 3.将exchange与队列绑定
	 * 如上三部可以省略，上线的时候一般是申请创建的
	 * @return
	 */
	//1.生成exchange
	/*@Bean
	DirectExchange exchange() {
		return new DirectExchange(exchangeName, true, true);
	}*/
	/**
	 * 2.生成队列 
	 */
	/*@Bean
	public Queue queue() {
		return new Queue(queueName, true);//队列名称， 持久性标志
	}*/
	
	/**
	 * 3.将交换器 与  队列 进行绑定，并指定队列名称
	 */
	/*@Bean
	Binding binding() {
		return BindingBuilder.bind(queue()).to(exchange()).with(queueName);
	}*/

	/**
	 * RabbitTemplate配置
	 * @return
	 */
	/*@Bean
	public ExponentialBackOffPolicy backOffPolicy(){
		ExponentialBackOffPolicy backOffPolicy =  new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(500);
		backOffPolicy.setMaxInterval(1000);
		backOffPolicy.setMultiplier(10.0);
		return backOffPolicy;
	}
	@Bean
	public RetryTemplate retryTemplate(){
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setBackOffPolicy(backOffPolicy());
		return retryTemplate;
	}*/
	@Bean
	public RabbitTemplate rabbitTemplate() {
		RabbitTemplate template = new RabbitTemplate(connectionFactory());
		//设置exchange到queue失败,则回调returnCallback(需设置mandatory=true,否则不回回调,消息就丢了)
		template.setMandatory(true);
		template.setReturnCallback((message, replyCode, replyText, tmpExchange, tmpRoutingKey) -> {
			logger.info("ReturnCallback start ....");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			logger.info("send message failed: " + replyCode + " " + replyText);
			//消息丢队列失败，可以制定处理方案，比如重发消息
		});
		return template;
	}
	

	

}
