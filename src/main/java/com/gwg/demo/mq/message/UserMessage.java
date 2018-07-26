package com.gwg.demo.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMessage{
	
	 int id;
	 String name;
	 Date birthday;

}
