package com.gwg.demo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.alibaba.fastjson.JSON;

public class Example {
	
	public static void main(String[] args) {
		List<String> list = new ArrayList<String>();
		list.add("gao");
		list.add("wei");
		list.add("gang");
		Iterator<String> itr = list.iterator();
		while(itr.hasNext()){
			String str = itr.next();
			if("gao".equals(str)){
				itr.remove();//删除list集合里面的数据
			}
		}
		System.out.println(JSON.toJSON(itr));
		System.out.println(JSON.toJSON(list));
		
	}

}
