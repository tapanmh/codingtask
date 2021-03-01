package com.example.orderbook.service;

public class OrderIdService {

	/**
	 * An auto incremented sequential number.
	 **/
	private static Long currentOrderId = 0L;
	private static OrderIdService instance;

	private OrderIdService(){};// private constructor, to prevant instantiation of another copy

	public static OrderIdService getInstance() {
		if (instance == null) {
			synchronized (OrderIdService.class){
				if (instance == null) {
					instance = new OrderIdService();
				}
			}
		}
		return instance ;
	}
	
	/**
	 * This method generates a system-wide unique orderId, in sequence and auto-incremented
	 * **/
	public Long getId() {
		synchronized(currentOrderId){
			return currentOrderId++;
		}
	}

}
