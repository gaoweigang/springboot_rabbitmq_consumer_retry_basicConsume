package com.gwg.demo.mq.consumer.process;

import com.gwg.demo.mq.common.DetailResult;

public interface MessageProcess<T> {
	
    public DetailResult process(T messageBean);
}

