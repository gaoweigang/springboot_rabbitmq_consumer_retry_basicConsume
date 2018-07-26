package com.gwg.demo.mq.consumer.message.process;

import com.gwg.demo.mq.common.DetailResult;

public interface MessageProcess<T> {
	
    public DetailResult process(T messageBean);
}

