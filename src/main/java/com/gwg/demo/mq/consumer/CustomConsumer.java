package com.gwg.demo.mq.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import com.alibaba.fastjson.JSON;
import com.gwg.demo.mq.common.Action;
import com.gwg.demo.mq.common.DetailResult;
import com.gwg.demo.mq.consumer.message.process.MessageProcess;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class CustomConsumer<T> extends DefaultConsumer{
	
	private static final Logger logger = LoggerFactory.getLogger(CustomConsumer.class);
	//设置Message的序列化方法
	private MessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();
	private MessageConverter messageConverter = new Jackson2JsonMessageConverter();
	
	private Channel channel;
	
	private Connection connection;
	
	private MessageProcess messageProcess;

	public CustomConsumer(Channel channel, Connection connection, MessageProcess messageProcess) {
		super(channel);
		this.channel = channel;
		this.connection = connection;
		this.messageProcess = messageProcess;
	}


	@Override
	public void handleDelivery(String consumerTag, Envelope envelope,
			BasicProperties properties, byte[] body) throws IOException {
		Action action = Action.RETRY;
		
		
	    
		try {
			Thread.sleep(2000);
			//String message = new String(body, "UTF-8");
			Message message = new Message(body, messagePropertiesConverter
					.toMessageProperties(properties, envelope, "UTF-8"));
			logger.info("消息内容：{}", message);
			T messageBean = (T) messageConverter.fromMessage(message);
			logger.info("consume 消息处理 start....，消息内容：{}", JSON.toJSON(messageBean));
			DetailResult result = messageProcess.process(messageBean);
			if(result.isSuccess()){//
				logger.info("消费成功 返回确认消息....");
				action = Action.ACCEPT;
				//channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
			}else{
				logger.info("消费失败,消息重新入队....");
				action = Action.RETRY;
				//channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);//前提是需要设置消息确认模式为手动，否则无效
			}
		} catch (MessageConversionException e) {
			// TODO Auto-generated catch block
			action = Action.RETRY;
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}  finally{
			try {
				if(action == Action.ACCEPT){
					//前提是需要在basicGet()的时候设置消息确认模式为手动，否则无效
					channel.basicAck(envelope.getDeliveryTag(), false);
				}else if(action == Action.RETRY){
					//前提是需要在basicGet()的时候设置消息确认模式为手动，否则无效
					channel.basicNack(envelope.getDeliveryTag(), false, true);//消息重新入队
				}else if(action == Action.REJECT){
					//前提是需要在basicGet()的时候设置消息确认模式为手动，否则无效
					channel.basicNack(envelope.getDeliveryTag(), false, false);//丢弃消息
				}
				channel.close();

			} catch (IOException e) {
				try {
					channel.close();
				} catch (TimeoutException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				channel = connection.createChannel(false);
				e.printStackTrace();
			} catch (TimeoutException e) {
				try {
					channel.close();
				} catch (TimeoutException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				channel = connection.createChannel(false);
				e.printStackTrace();
			}
		}
	}
}
