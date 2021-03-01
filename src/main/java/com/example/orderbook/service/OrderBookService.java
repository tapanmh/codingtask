package com.example.orderbook.service;

import com.example.orderbook.*;
import com.example.orderbook.model.Order;
import com.example.orderbook.model.request.OrderRequest;
import com.example.orderbook.model.request.OrderType;
import com.example.orderbook.model.request.UpdateOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OrderBookService is a service class for booking new order, updating an order, list orders, list orders by security , cancel order
 */

@Service
public class OrderBookService {

	Logger logger = LoggerFactory.getLogger(OrderBookService.class);
	@Autowired
    PriceTimePriorityOrderBook orders;
    private static final Double HIGH_VALUE = 9999999999.0;
    private static final Double LOW_VALUE = 0.0;

	public List<Order> listOrders(){
		final List<Order> orders =  this.orders.getAllOrders();
		return orders;
	}

	public List<Order> listOrdersBySecurity(String securityId){
		final List<Order> orders =  this.orders.getAllOrdersBySecurity(securityId);
		return orders;
	}


	public String bookOrder(OrderRequest orderRequest)
	{
		Double value = null;
        /**
         * Setting nominal values in case of Market_order
         * for buy orders setting a high value and for sell order setting low value,
         * these are only to maintain their preferable top position in the orderbook,
         * these nominal values are not used in calculation
         */
		if(orderRequest.getOrderType().equals(OrderType.MARKET_ORDER)){
			if(orderRequest.isBuying()){
                value = HIGH_VALUE;
            }else{
                value = LOW_VALUE;
            }
		}else{
			value = orderRequest.getValue();
		}


		Order order = new Order(orderRequest.getClientId(),orderRequest.getSecurityId(),
				orderRequest.getUnits(), value,
				orderRequest.isBuying(),orderRequest.getOrderType(), System.currentTimeMillis());
		Double transactionValue;
		if(orderRequest.isBuying()){
			logger.info("BUY ORDER RECEIVED WITH FOLLOWING DETAILS : {} " , order);
			transactionValue = orders.buyTrade(order);
		}else{
			logger.info("SELL ORDER RECEIVED WITH FOLLOWING DETAILS : {} " , order);
			transactionValue = orders.sellTrade(order);
		}
		return "Order Received - " + transactionValue;
	}

	public String updateOrder(UpdateOrderRequest updateOrderRequest)  {
		Order retrievedOrder = orders.findOrderByOrderId(updateOrderRequest.getOrderId());

		Double value = null;
		if(retrievedOrder.getOrderType().equals(OrderType.MARKET_ORDER)){
			if(retrievedOrder.isBuying()){
				value = HIGH_VALUE;
			}else{
				value = LOW_VALUE;
			}
		}else{
			value = updateOrderRequest.getValue();
		}

		Order order = new Order(retrievedOrder.getOrderId(), retrievedOrder.getClientId(),retrievedOrder.getSecurityId(),
				updateOrderRequest.getUnits(), value,
				retrievedOrder.isBuying(),retrievedOrder.getOrderType(), System.currentTimeMillis());

		logger.info("INSTRUCTIONS TO UPDATE AN ORDER RECEIVED WITH FOLLOWING DETAILS : {} " , order);
		Double transactionValue = orders.update(order);
		return "Order Received for Update - " + transactionValue;

	}



	public void cancelOrder(Long orderId){
		orders.cancelOrder(orderId);
	}

}
