package com.gwg.demo.mq.common;

public interface MessageProducer<T> {
	
	public DetailResult produce(T message);

}
