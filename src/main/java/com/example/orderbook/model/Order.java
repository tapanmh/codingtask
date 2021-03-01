package com.example.orderbook.model;

import com.example.orderbook.model.request.OrderType;
import com.example.orderbook.service.OrderIdService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;


/**
 * Represents a buy or sell Order. handles concurrent users by synchronizing the
 * amount of securities placed in this order in get and set methods
 * Handles updating an order by means of a unique orderId
 * priorityTime is used for ordering, displayTime  is used for latest update time.
 */
public class Order implements Serializable {
	private static final long serialVersionUID = 8822833371248140397L;

	private Long orderId;
	private String clientId;
	private String securityId;
	private Integer units;
	private Double value;
	private Boolean isBuying;
	private OrderType orderType;
	private Long priorityTime;
	private Long displayTime;

	/**
	 *  This constructor is for new order creation, it generates a new orderId
	 */
	public Order (String clientId, String securityId, Integer units, Double value, boolean isBuying, OrderType orderType, long timestamp){
		this.orderId = OrderIdService.getInstance().getId();
		this.clientId = clientId;
		this.securityId = securityId;
		this.units = units;
		this.value = value;
		this.isBuying = isBuying;
		this.orderType = orderType;
		this.priorityTime = timestamp;
		this.displayTime = timestamp;

	}


	/**
	 *  This constructor is for order updation, it accepts orderId as a parameter
	 */
	public Order (Long orderId, String clientId, String securityId, Integer units, Double value, boolean isBuying, OrderType orderType, long timestamp){
		this.orderId = orderId;
		this.clientId = clientId;
		this.securityId = securityId;
		this.units = units;
		this.value = value;
		this.isBuying = isBuying;
		this.orderType = orderType;
		this.priorityTime = timestamp;
		this.displayTime = timestamp;

	}


	public Long getOrderId() {
		return orderId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getSecurityId() {
		return securityId;
	}

	public Integer getUnits() {
		synchronized(units){
			return units;
		}
	}

	public void setUnits(Integer units) {
		synchronized(units){
			this.units = units;
		}
	}

	public void setDisplayTime(Long milliseconds) {
		synchronized(displayTime){
			this.displayTime = milliseconds;
		}
	}

	public Double getValue() {
		return value;
	}

	public boolean isBuying() {
		return isBuying;
	}
	public OrderType getOrderType(){
		return orderType;
	}

	public Long getDisplayTime() {
		synchronized(displayTime){
			return displayTime;
		}
	}

	public Long getPriorityTime() {
		synchronized(priorityTime){
			return priorityTime;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orderId == null) ? 0 : orderId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (orderId == null) {
			if (other.orderId != null)
				return false;
		} else if (!orderId.equals(other.orderId))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "ORDERID="+ orderId +" CLIENT=" + clientId + " SECURITY=" + securityId
				+ " UNITS=" + units + " VALUE=" + value + " ISBUYING="
				+ (isBuying? "YES":"NO") + ", TIMESTAMP=" + displayTime + ", ORDERTYPE= " + orderType ;
	}

}
