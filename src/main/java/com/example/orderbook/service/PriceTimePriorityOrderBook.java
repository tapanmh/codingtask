package com.example.orderbook.service;

import com.example.orderbook.model.request.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import com.example.orderbook.model.Order;

/**
 * PriceTimePriorityOrderBook is an price time order matching engine that automatically matches viable transactions
 * unmatched orders are stored in order
 *
 * uses Java PriorityBlockingQueue
 *
 * For handling concurrent clients, following operations are handled
 * 1. Adding or removing a new order for a given security is handled with the individual PriorityBlockingQueues.
 * 2. Lookup of a given security is handled by ConcurrentHashMap
 * 3. Partial fulfillment of an order is handled with the setUnits method of Order class
 */
@Service
public class PriceTimePriorityOrderBook {

	private static final int INITIAL_CAPACITY = 10;
	private Map<String,PriorityBlockingQueue<Order>> buyMap;
	private Map<String,PriorityBlockingQueue<Order>> sellMap;

	Logger logger = LoggerFactory.getLogger(PriceTimePriorityOrderBook.class);

    private static final Double HIGH_VALUE = 9999999999.0;
    private static final Double LOW_VALUE = 0.0;

    public PriceTimePriorityOrderBook() {
		this.buyMap = new ConcurrentHashMap<String, PriorityBlockingQueue<Order>>();
		this.sellMap = new ConcurrentHashMap<String, PriorityBlockingQueue<Order>>();
	}

	/**
	 * Attempts to match (sell) an order or queues it until a buyer arrives.
	 * 
	 * @param sellOrder
	 * 			the sell order
	 * @return
	 * 		The effective transaction value, or null if queued.
	 */
	public Double sellTrade(Order sellOrder){
		if(sellOrder.isBuying() || sellOrder.getUnits() <= 0){
			throw new IllegalArgumentException("selling a BUY order");
		}
		String desiredSecurity = sellOrder.getSecurityId();		
		Double transactionValue = 0.0;
		PriorityBlockingQueue<Order> buyQueueForSecurity = buyMap.get(desiredSecurity);
		if(buyQueueForSecurity != null){
			sameBuyerSellerCheck(buyQueueForSecurity, sellOrder);
			transactionValue = matchOrder(buyQueueForSecurity, sellOrder);
		}
		//2. If we still have sell units (i.e no match or partially fulfilled it), queue it.
		if(sellOrder.getUnits() > 0){
			if(sellMap.containsKey(desiredSecurity)){
				sellMap.get(desiredSecurity).offer(sellOrder);
			}else{
				//Critical section: creating and adding a new queue for an non-existing security.
				PriorityBlockingQueue<Order> pq = new PriorityBlockingQueue<Order>(INITIAL_CAPACITY, new SellSideComparator());
				pq.offer(sellOrder);
				sellMap.put(desiredSecurity, pq);
			}
		}
		if(transactionValue == 0.0){
			logger.info("SALE ORDER QUEUED {}", sellOrder.getOrderId().toString());
		}
		return transactionValue;
	}

	/**
	 * Attempts to match (buy) an order or queues it until a seller arrives.
	 * 
	 * @param buyOrder
	 * 			the order to be bought
	 * @return
	 * 		The effective transaction value, or 0 if queued.
	 */
	public Double buyTrade(Order buyOrder) {
		if(!buyOrder.isBuying() || buyOrder.getUnits() <= 0){
			throw new IllegalArgumentException("buying a SELL order");
		}

		String desiredSecurity = buyOrder.getSecurityId();
		Double transactionValue = 0.0;
		PriorityBlockingQueue<Order> sellQueueForSecurity = sellMap.get(desiredSecurity);
		if(sellQueueForSecurity != null){
			sameBuyerSellerCheck(sellQueueForSecurity, buyOrder);
			transactionValue = matchOrder(sellQueueForSecurity, buyOrder);
		}
		if(buyOrder.getUnits() > 0){
			if(buyMap.containsKey(desiredSecurity)){
				buyMap.get(desiredSecurity).offer(buyOrder);
			}else{
				//creating and adding a new queue for an non-existing security.
				PriorityBlockingQueue<Order> pq = new PriorityBlockingQueue<Order>(INITIAL_CAPACITY, new BuySideComparator());
				pq.offer(buyOrder);
				buyMap.put(desiredSecurity, pq);
			}
		}
		if(transactionValue == 0.0){
			logger.info("BUY ORDER QUEUED {}", buyOrder.getOrderId().toString());
		}
		return transactionValue;
	}

	// Buyer and seller cannot be the same person for the same security
	private void sameBuyerSellerCheck(PriorityBlockingQueue<Order> pq, Order order){
		List<Order> st = pq.stream()
				.filter(o -> o.getClientId().equals(order.getClientId())).collect(Collectors.toList());
		if(st.size() >0){
			String msg = order.getClientId()+
					" is Trying to buy and Sell the same security, This is not permitted";
			System.err.println(msg);
			logger.warn("msg");
			throw new IllegalArgumentException(msg);
		}
	}

	/**
	 * Attempts to match a buy order with sell order and vice versa
	 *
	 * @param pq
	 * 			queue holding orders for a given security
	 * @param o
	 *			buy/sell order
	 * @return
	 * 		The effective transaction value, or 0 if queued.
	 */
	private Double matchOrder(PriorityBlockingQueue<Order> pq, Order o){
		Order bestCandidate = pq.peek();
		if(bestCandidate == null || o.getUnits() == 0){
			return 0.0;
		}
		String security = o.getSecurityId();
		Double transactionValue = bestCandidate.getValue();	
		int placedUnits = 0;
		boolean shouldMakeTransaction = o.isBuying()?
				(o.getValue() >= transactionValue):
					(o.getValue() <= transactionValue);
				//for market_order offer the best trade available at that point in time
				if(o.getOrderType().equals(OrderType.MARKET_ORDER)){
					shouldMakeTransaction = true;
				}

				if(	shouldMakeTransaction){
					int oUnits = o.getUnits();
					int bestCandidateUnits = bestCandidate.getUnits();
                    //
                    transactionValue = bestCandidate.getValue();
                    //for bestCandidate is an MARKET_ORDER , values from bid/ask order should be considered
                    if(bestCandidate.getOrderType().equals(OrderType.MARKET_ORDER)){
                        if(bestCandidate.getValue().equals(HIGH_VALUE) || bestCandidate.getValue().equals(LOW_VALUE)) {
                            transactionValue = o.getValue();
                        }
                    }
                    //

                    //If both the bestcandidate and placed order are MARKET_ORDER, then find the security market value based on
                    //the price of next best limit order from order book
                    if(bestCandidate.getOrderType().equals(OrderType.MARKET_ORDER) &&
                            o.getOrderType().equals(OrderType.MARKET_ORDER)){
                        transactionValue = getMarketPrice(pq);
                    }

					if(oUnits > bestCandidateUnits){
						placedUnits = bestCandidateUnits;
						o.setUnits(oUnits - bestCandidateUnits);
						logger.info("ORDER MATCHED - security: " + security + "  placedUnits : " + placedUnits + "  transactionValue : " + transactionValue + "  o.isBuying() : " + o.isBuying() );
						bestCandidate.setUnits(0);				
						pq.remove(bestCandidate);
					}else if(oUnits < bestCandidateUnits){
						placedUnits = oUnits;
						o.setUnits(0);
						logger.info("ORDER MATCHED - security: " + security + "  placedUnits : " + placedUnits + "  transactionValue : " + transactionValue + "  o.isBuying() : " + o.isBuying() );
						bestCandidate.setUnits(bestCandidateUnits - oUnits);				
					}else{
						placedUnits = oUnits;//either one... 
						o.setUnits(0);				
						logger.info("ORDER MATCHED - security: " + security + "  placedUnits : " + placedUnits + "  transactionValue : " + transactionValue + "  o.isBuying() : " + o.isBuying() );
						bestCandidate.setUnits(0);
					pq.remove(bestCandidate);
					}
					//If we still have units, attempt to match recursively
					return transactionValue * placedUnits + matchOrder(pq,o);
				}
				return placedUnits > 0 ? transactionValue: 0.0;
	}

	/**
	 * clear orders when trading session closes
	 */
	public void clear() {
		buyMap.clear();
		sellMap.clear();		
	}

	/**
	 * removes all orders for a clientid
	 * @param clientId
	 * 			The clientId
	 */

	public void remove(String clientId) {
		removeFromMap(clientId, buyMap);
		removeFromMap(clientId, sellMap);
	}

	/**
	 * cancels an order for a given orderid
	 * @param orderId
	 * 			The orderId
	 */
	public void cancelOrder(Long orderId) {
		logger.info("received cancellation for order id  {} " , orderId);
		removeOrderFromMap(orderId, buyMap);
		removeOrderFromMap(orderId, sellMap);
	}

	/**
	 * Removes an order from a given map
	 * @param orderId
	 * 			The orderId
	 * @param map
	 * 			The map from which we want to remove the orderId.
	 */
	private void removeOrderFromMap(Long orderId, Map<String,PriorityBlockingQueue<Order>> map){
		Set<String> keys = map.keySet();
		for (String key : keys) {
			PriorityBlockingQueue<Order> securitiesForKey = map.get(key);
			securitiesForKey.removeIf(o -> o.getOrderId().equals(orderId));
		}
	}


	/**
	 * returns an order based on provided orderid
	 *
	 * @param orderId
	 * 			The orderId
	 * @return
	 * 			an order
	 */
	public Order findOrderByOrderId(Long orderId){
		Order orderFound = null;
		boolean success = false;
		Set<String> keys = buyMap.keySet();
		for (String key : keys) {
			PriorityBlockingQueue<Order> securitiesForKey = buyMap.get(key);
			for (Order order : securitiesForKey) {
				if(order.getOrderId().equals(orderId)){
					orderFound = order;
					success = true;
					break;
				}
			}
		}
		if(success) return orderFound;
		keys = sellMap.keySet();
		for (String key : keys) {
			PriorityBlockingQueue<Order> securitiesForKey = sellMap.get(key);

			for (Order order : securitiesForKey) {
				if(order.getOrderId().equals(orderId)){
					orderFound = order;
					success = true;
					break;
				}
			}
		}
		return orderFound;

	}


	/**
	 * Removes from a given map, all orders that where placed by a client.
	 * @param clientId
	 * 			The clients unique identifier.
	 * @param map
	 * 			The map for which we want to remove.
	 */
	private void removeFromMap(String clientId, Map<String,PriorityBlockingQueue<Order>> map){
		Set<String> keys = map.keySet();
		for (String key : keys) {
			PriorityBlockingQueue<Order> securitiesForKey = map.get(key);
			securitiesForKey.removeIf(o -> o.getClientId().equals(clientId));
		}	
	}


	/**
	 * list all orders
	 * @return list
	 * 			a list of all orders
	 */
	public List<Order> getAllOrders(){
		List<Order> list = new LinkedList<Order>();
		createMap(list, buyMap);
		createMap(list, sellMap);
		return list;
	}

	/**
	 * returns all orders for a given securityid
	 *
	 * @param securityId
	 * 			The security id
	 * @return
	 * 			all orders for a given securityid
	 */
	public List<Order> getAllOrdersBySecurity(String securityId){
		List<Order> list = new LinkedList<Order>();
		createMapBySecurity(list,securityId, buyMap);
		createMapBySecurity(list,securityId, sellMap);
		return list;
	}

	/**
	 * returns market price
	 *
	 * @param pq
	 * 			The PriorityBlockingQueue
	 * @return
	 * 			an market price
	 */
    public Double getMarketPrice(PriorityBlockingQueue<Order> pq){
        Double returnValue = 0.0;
        for (Order order : pq) {
            if(order.getOrderType().equals(OrderType.LIMIT_ORDER)){
                returnValue  = order.getValue();
                 break;
            }
        }
        return returnValue;
    }


	/**
	 * Updates an existing order in the queue following this criteria:
	 * 1.	quantity decreases, price equals, keep priority - in-place
	 * 2.	price changes, remove add
     * 3.	quantity increases, price equals, remove add. (lose priority)
	 * @param orderToUpdate
	 * @return 
	 */
	public Double update(Order orderToUpdate){
		Map<String,PriorityBlockingQueue<Order>> sideToUpdateMap;
		if(orderToUpdate.isBuying()){
			sideToUpdateMap = buyMap;
		}else{
			sideToUpdateMap = sellMap;
		}
		PriorityBlockingQueue<Order> securitiesForKey = sideToUpdateMap.get(orderToUpdate
				.getSecurityId());
		Order removeAddOrder = null;
		Double retVal = 0.0;
		boolean success = false;
		for (Order order : securitiesForKey) {
			if(order.getOrderId().equals(orderToUpdate.getOrderId())){
				if(orderToUpdate.getValue().equals(order.getValue())){
					if(orderToUpdate.getUnits() < order.getUnits()){
						//CASE 1. quantity decreases, price equals, keep priority - in-place
						order.setUnits(orderToUpdate.getUnits());
						order.setDisplayTime(orderToUpdate.getDisplayTime());
						success = true;
						break;
					}
				}
				//CASE 2.price changes, remove and add
				//CASE 3.quantity increases, price equals, remove and add. (lose priority)
				removeAddOrder = order;
				break;
			}
		}
		if(removeAddOrder != null){
			securitiesForKey.remove(removeAddOrder);
			if(orderToUpdate.isBuying()){
				retVal = buyTrade(orderToUpdate);
			}else{
				retVal = sellTrade(orderToUpdate);
			}
			success = true;
		}
		logger.info("ORDER UPDATED : orderid - " + orderToUpdate.getOrderId().toString() + " success - " + success);
		return retVal;
	}



	/**
	 * clones all orders from map into Collection respecting it's actual priority in the queue.
	 * Note that doing this requires duplicating each queue given that the only real way
	 * to know the ordering is by using poll, which is destructive is used in our internal state.
	 * @param collection
	 * 			a set to add all orders contained by the map
	 * @param map
	 * 			buy/sell map to be dumped.
	 */
	private void createMap(Collection<Order> collection , Map<String,PriorityBlockingQueue<Order>> map){
		Set<String> keys = map.keySet();
		for (String key : keys) {
			PriorityBlockingQueue<Order> securitiesForKey = map.get(key);
			PriorityBlockingQueue<Order> securitiesForKeyClone = new PriorityBlockingQueue<Order>(INITIAL_CAPACITY, 
					securitiesForKey.comparator());
			for (Order order : securitiesForKey) {
				securitiesForKeyClone.offer(order);
			}
			while(!securitiesForKeyClone.isEmpty()){
				Order polled = securitiesForKeyClone.poll();
				collection.add(polled);
			}
		}
	}


	/**
	 * clones all orders from map into Collection respecting it's actual priority in the queue for a given securityid
	 * Note that doing this requires duplicating each queue given that the only real way
	 * to know the ordering is by using poll, which is destructive is used in our internal state.
	 * @param collection
	 * 			a set to add all orders contained by the map
	 * @param securityId
	 * 			securityId for which the map to be created
	 *  @param map
	 * 			buy/sell map to be dumped.
	 */

	private void createMapBySecurity(Collection<Order> collection , String securityId, Map<String,PriorityBlockingQueue<Order>> map){
		Set<String> keys = map.keySet();
		for (String key : keys) {
			if(key.equalsIgnoreCase(securityId)) {
				PriorityBlockingQueue<Order> securitiesForKey = map.get(key);
				PriorityBlockingQueue<Order> securitiesForKeyClone = new PriorityBlockingQueue<Order>(INITIAL_CAPACITY,
						securitiesForKey.comparator());
				for (Order order : securitiesForKey) {
					securitiesForKeyClone.offer(order);
				}
				while (!securitiesForKeyClone.isEmpty()) {
					Order polled = securitiesForKeyClone.poll();
					collection.add(polled);
				}
			}
		}
	}



	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("----------toString----------\n");
		sb.append("BUYING: \n");
		List<Order> buyingOrders = new LinkedList<Order>();
		createMap(buyingOrders, buyMap);
		for (Order order : buyingOrders) {
			sb.append(order.toString()+"\n");
		}
		sb.append("SELLING: \n");
		List<Order> sellingOrders = new LinkedList<Order>();
		createMap(sellingOrders, sellMap);
		for (Order order : sellingOrders) {
			sb.append(order.toString()+"\n");
		}
		sb.append("----------END-toString----------\n");
		return sb.toString();
	}

	static class BuySideComparator implements Comparator<Order> {
		/**
		 * The orders are listed Highest to Lowest on the Buy Side, we do the opposite of natural order on the value.
		 * Returns: a positive integer if Order one is of LESS value than Order two.
		 * A negative integer if Order one is of GREATER value than Order two.
		 * or, if they are of the same value, it prioritizes orders that arrived earlier. 
		 */
		public int compare(Order one, Order two) {
			if(!one.getSecurityId().equals(two.getSecurityId())){
				System.err.println("These orders are not comparable, they need to be for the same security");
				new IllegalArgumentException();
			}
			int naturalOrder = Double.compare(one.getValue() , two.getValue());
			if(naturalOrder == 0){
				return   Long.compare(one.getPriorityTime(), two.getPriorityTime());
			}
			return -(naturalOrder);
		}
	}

	static class SellSideComparator implements Comparator<Order> {

		/**
		 * Because orders are listed Lowest to Highest on the Sell Side, we use natural ordering.
		 * Returns: a positive integer if Order one is of GREATER value than Order two.
		 * a negative integer if Order one is of LESS value than Order two.
		 * or, if they are of the same value, it prioritizes orders that arrived earlier. 
		 */
		public int compare(Order one, Order two) {
			if(!one.getSecurityId().equals(two.getSecurityId())){
				System.err.println("These orders are not comparable, they need to be for the same security");
				new IllegalArgumentException();
			}
			int naturalOrder = Double.compare(one.getValue() , two.getValue());
			if(naturalOrder == 0){
				return   Long.compare(one.getPriorityTime(), two.getPriorityTime());
			}
			return naturalOrder;
		}
	}



}
