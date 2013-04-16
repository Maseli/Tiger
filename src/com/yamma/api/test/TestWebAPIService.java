package com.yamma.api.test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import org.apache.http.impl.client.DefaultHttpClient;

public class TestWebAPIService extends Thread {
	
	private static final int limit = 100;
	private static final int number = 100;
	
	private static Map<String, Integer> result = new ConcurrentHashMap<String,Integer>();
	
	public static void main(String[] args) {
		new TestWebAPIService().start();
		new TestWebAPIService().start();		
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();		
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();		
		new TestWebAPIService().start();
		new TestWebAPIService().start();		
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();		
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();
		new TestWebAPIService().start();
	}
	
	@Override
	public void run() {
		
		int i = 0;
		Date start = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss-SS");
		
		Random random = new Random();
		while (true) {
			// 核心应用类
			HttpClient httpClient = new DefaultHttpClient();
			String receiver = "yamma"+(Math.abs((random.nextInt()%number))+1);
			result.put(receiver, ((result.get(receiver)!=null)?result.get(receiver):0)+1);
			// HTTP请求
			HttpUriRequest request = new HttpGet(
					"http://192.168.0.55:8080/sn/?receiver="+receiver+"&notify="+sdf.format(new Date())+"%20%20yamma");
			// 打印请求信息
//			System.out.println(request.getRequestLine());
			try {
				// 发送请求，返回响应
				HttpResponse response = httpClient.execute(request);
				// 打印响应信息
//				System.out.println(response.getStatusLine());
				if(++i==limit)
					break;
			} catch (ClientProtocolException e) {
				// 协议错误
				e.printStackTrace();
			} catch (IOException e) {
				// 网络异常
				e.printStackTrace();
			}
		}
		
		for(int t=1;t<=number;t++) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpUriRequest request = new HttpGet(
					"http://192.168.0.55:8080/sn/?receiver=yamma"+t+"&notify="+"end!");
			try {
				HttpResponse response = httpClient.execute(request);
				i++;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Date end = new Date();
		System.out.println("发送条数为:"+i+" 花费时间："+(double)(end.getTime()-start.getTime()));
		
		System.out.println(result);
	}
}
