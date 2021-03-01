package com.example.orderbook.service;

import static org.junit.Assert.assertEquals;

import java.rmi.RemoteException;
import java.util.List;

import com.example.orderbook.model.request.OrderType;
import org.junit.Before;
import org.junit.Test;

import com.example.orderbook.model.Order;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class PriceTimePriorityOrderBookTest {

	private static final String TRADER_SELLER_1 = "TRADER_SELLER_1";
	private static final String TRADER_SELLER_2 = "TRADER_SELLER_2";
	private static final String TRADER_BUYER_1 = "TRADER_BUYER_1";
	private static final String TRADER_BUYER_2 = "TRADER_BUYER_2";

	private static final String SECURITY_1 = "REL";
	private static final String SECURITY_2 = "TATA";
	private PriceTimePriorityOrderBook orderbook;

	@Before
	public final void setUp() {
		orderbook = new PriceTimePriorityOrderBook();
	}
	/**
	 * Two similar sale orders are placed, wait for a valid buyer arrival.
	 * One buy order placed, that is matched to first sale order
	 * Expected Result: The first sale order placed should be executed based on timestamp
	 */
	@Test
	public void testSaleOrders() {
		Order saleOrder1 = new Order(TRADER_SELLER_1, SECURITY_1, 5, 50.0,
				false , OrderType.LIMIT_ORDER, System.currentTimeMillis());
		Order saleOrder2 = new Order(TRADER_SELLER_2, SECURITY_1, 5, 50.0,
				false, OrderType.LIMIT_ORDER , System.currentTimeMillis());

		orderbook.sellTrade(saleOrder1);
		orderbook.sellTrade(saleOrder2);

		Order buyOrder = new Order(TRADER_BUYER_1, SECURITY_1, 5, 50.0,
				true , OrderType.LIMIT_ORDER , System.currentTimeMillis());
		orderbook.buyTrade(buyOrder);

		List<Order> remainingOrders = orderbook.getAllOrders();
		//one order should be remaining in the book
		assertEquals(1, remainingOrders.size());
		//The remaining order should be the sale order2, because the first one was executed
		assertEquals(saleOrder2 , remainingOrders.iterator().next());
	}


	/**
	 * Two similar purchase orders are placed, wait for a valid seller arrival.
	 * One sale order placed, that is matched to first purchase order
	 * Expected Result: The first purchase order placed should be executed based on timestamp
	 */
	@Test
	public void testPurchaseOrders()  {
		Order buyOrder1 = new Order(TRADER_BUYER_1, SECURITY_1, 5, 50.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis());
		Order buyOrder2 = new Order(TRADER_BUYER_2, SECURITY_1, 5, 50.0,
				true, OrderType.LIMIT_ORDER , System.currentTimeMillis());



		orderbook.buyTrade(buyOrder1);
		orderbook.buyTrade(buyOrder2);

		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 5, 50.0,
				false , OrderType.LIMIT_ORDER , System.currentTimeMillis());

		orderbook.sellTrade(sellOrder);
		List<Order> remainingOrders = orderbook.getAllOrders();
		//one order should be remaining in the book, The remaining order should be the sale order2 because the first one was executed
		assertEquals(1, remainingOrders.size());
		assertEquals(buyOrder2,remainingOrders.iterator().next());
	}


	/**
	 * A seller places an order for (100) less than the lowest buyer bid queued.(150)
	 * Expected Result: The Trade tx should occur for the highest bidder (150)
	 */
	@Test
	public void testSellerGetsMore() {
		Order buyOrder = new Order(TRADER_BUYER_1, SECURITY_1, 1, 150.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis());

		assertEquals(new Double(0.0), orderbook.buyTrade(buyOrder));

		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 100.0,
				false ,OrderType.LIMIT_ORDER, System.currentTimeMillis());

		Double transactionValue = orderbook.sellTrade(sellOrder);
		assertEquals(new Double(150.0), transactionValue);
	}

	/**
	 *
	 * Partial sell order
	 * A buy order arrives for less units than a queued applicable sale order.
	 *
	 * Expected: We partially fulfill the order thus satisfying the buyer
	 * and partially completing the sellers order.
	 * @throws RemoteException
	 */
	@Test
	public void testPartialSale()  {
		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 200, 9.0,
				false , OrderType.LIMIT_ORDER, System.currentTimeMillis() );

		orderbook.sellTrade(sellOrder);

		Order buyOrder = new Order(TRADER_BUYER_1, SECURITY_1, 100, 10.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis() );
		orderbook.buyTrade(buyOrder);
		List<Order> remainingOrders = orderbook.getAllOrders();
		assertEquals(1, remainingOrders.size());
		Order remainingOrder = remainingOrders.iterator().next();
		assertEquals(new Integer(100), remainingOrder.getUnits());
	}


	/**
	 * Buyer places an order for price(150) that is more than highest offer(100) available in queue
	 * Expected Result: The trade tx should occur at the highest offer (100) which is less than what the buyer actually placed
	 */
	@Test
	public void testBuyerPaysLess() {
		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 100.0,
				false ,OrderType.LIMIT_ORDER, System.currentTimeMillis());

		assertEquals(new Double(0.0), orderbook.sellTrade(sellOrder));

		Order buyOrder = new Order(TRADER_BUYER_1, SECURITY_1, 1, 150.0,
				true , OrderType.LIMIT_ORDER,System.currentTimeMillis());

		Double transactionValue = orderbook.buyTrade(buyOrder);
		assertEquals(new Double(100.0), transactionValue);
	}



	/**
	 * Test for partial buy order
	 * A sell order arrives for less units than a queued applicable buy order.
	 *
	 * Result Expected: order partially fulfilled
	 */
	@Test
	public void testPartialBuy() {
		Order buyOrder = new Order(TRADER_BUYER_1, SECURITY_1, 200, 10.0,
				true ,OrderType.LIMIT_ORDER, System.currentTimeMillis());

		orderbook.buyTrade(buyOrder);

		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 100, 10.0,
				false , OrderType.LIMIT_ORDER, System.currentTimeMillis());
		orderbook.sellTrade(sellOrder);

		List<Order> remainingOrders = orderbook.getAllOrders();
		//Partial fulfillment, buy order is partially remaining
		assertEquals(1, remainingOrders.size());

		Order remainingOrder = remainingOrders.iterator().next();
		assertEquals(new Integer(100), remainingOrder.getUnits());
	}



	/**
	 * A user places a sell order and then places buy order for the same security
	 */
	@Test(expected = IllegalArgumentException.class)
	public void TestSelfBuy() throws RuntimeException {
		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 1.0,
				false ,OrderType.LIMIT_ORDER, System.currentTimeMillis() );
		orderbook.sellTrade(sellOrder);

		Order buyOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 1.0,
				true , OrderType.LIMIT_ORDER,System.currentTimeMillis() );
		orderbook.buyTrade(buyOrder);
	}

	/**
	 *A user places a buy order and then places sell order for the same security
	 */
	@Test(expected = IllegalArgumentException.class)
	public void TestSelfSell() throws RuntimeException {
		Order buyOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 10.0,
				true ,OrderType.LIMIT_ORDER, System.currentTimeMillis());
		orderbook.buyTrade(buyOrder);

		Order sellOrder = new Order(TRADER_SELLER_1, SECURITY_1, 1, 9.0,
				false ,OrderType.LIMIT_ORDER, System.currentTimeMillis());
		orderbook.sellTrade(sellOrder);
	}




	/**
	 * Test for buy orders, orders should be sorted from high value to less value
	 * order with highest value should be on top of order book (preferred orders)
	 */
	@Test
	public void buySideComparator()  {
		PriceTimePriorityOrderBook.BuySideComparator comp = new PriceTimePriorityOrderBook.BuySideComparator();
		Order one = new Order(TRADER_BUYER_1, SECURITY_1, 1, 10.0,
				true , OrderType.LIMIT_ORDER, 1);

		Order two = new Order(TRADER_BUYER_2, SECURITY_1, 1, 10.0,
				true , OrderType.LIMIT_ORDER, 2);

		int equalButTimeWins = comp.compare(one, two);

		assertEquals(true, new Boolean(equalButTimeWins < 0 ) );


		one = new Order(TRADER_BUYER_1, SECURITY_1, 1, 10.0,
				true , OrderType.LIMIT_ORDER, 1);

		two = new Order(TRADER_BUYER_2, SECURITY_1, 1, 9.0,
				true , OrderType.LIMIT_ORDER, 1);

		int greaterThan = comp.compare(one, two);
		assertEquals(true, new Boolean(greaterThan < 0 ) );


		one = new Order(TRADER_BUYER_1, SECURITY_1, 1, 9.0,
				true ,OrderType.LIMIT_ORDER, 1);

		two = new Order(TRADER_BUYER_2, SECURITY_1, 1, 10.0,
				true , OrderType.LIMIT_ORDER, 1);

		int lessThan = comp.compare(one, two);
		assertEquals(true, new Boolean(lessThan > 0 ) );

	}

	/**
	 * Test for sell orders, orders should be sorted from less value to high value
	 * order with least value should be on top of orderbook (preferred orders)
	 */
	@Test
	public void sellSideComparator()  {
		PriceTimePriorityOrderBook.SellSideComparator comp = new PriceTimePriorityOrderBook.SellSideComparator();

		Order one = new Order(TRADER_SELLER_1, SECURITY_1, 1, 9.0,
				false , OrderType.LIMIT_ORDER, 1 );

		Order two = new Order(TRADER_SELLER_1, SECURITY_1, 1, 10.0,
				false , OrderType.LIMIT_ORDER,1 );

		int lessThan = comp.compare(one, two);

		assertEquals(true, new Boolean(lessThan < 0 ) );

		one = new Order(TRADER_SELLER_1, SECURITY_1, 1, 10.0,
				false ,OrderType.LIMIT_ORDER, 1 );

		two = new Order(TRADER_SELLER_2, SECURITY_1, 1, 10.0,
				false ,OrderType.LIMIT_ORDER, 2 );

		int equalButTimeWins = comp.compare(one, two);
		assertEquals(true, new Boolean(equalButTimeWins < 0 ) );


		one = new Order(TRADER_SELLER_1, SECURITY_1, 1, 10.0,
				false , OrderType.LIMIT_ORDER,1 );

		two = new Order(TRADER_SELLER_1, SECURITY_1, 1, 9.0,
				false ,OrderType.LIMIT_ORDER, 1);

		int greaterThan = comp.compare(one, two);
		assertEquals(true, new Boolean(greaterThan > 0 ) );

	}


	/**
	 *
	 * Multiple buy orders queued
	 * Sell order arrives
	 * Result : buy orders partially fulfilled
	 */
	@Test
	public void testPartialSaleWithMulipleOrders() {
		Order one = new Order(TRADER_BUYER_1, SECURITY_1, 500, 430.0,
				true , OrderType.LIMIT_ORDER, 1 );

		orderbook.buyTrade(one);

		Order two = new Order(TRADER_BUYER_2, SECURITY_1, 1000, 435.5,
				true ,OrderType.LIMIT_ORDER, 2 );
		orderbook.buyTrade(two);

		Order three = new Order(TRADER_SELLER_1, SECURITY_1, 1200, 429.0,
				false ,OrderType.LIMIT_ORDER, 3);

		Double transactionValue = orderbook.sellTrade(three);

		List<Order> remainingOrders = orderbook.getAllOrders();
		//order book should contain 1 order only
		assertEquals(1, remainingOrders.size());
		//remaining order is order1 , whereas order2 was fulfilled
		Order remainingOrder = remainingOrders.iterator().next();
		assertEquals(one , remainingOrder);
		//300 units are still pending in the order
		assertEquals(new Integer(300) , remainingOrder.getUnits());
		//trasaction value assertion
		assertEquals(new Double((435.5*1000) + (430.0*200)) ,transactionValue);
	}


	/**
	 * Order Update
	 * If price changes, priority is changed
	 */
	@Test
	public void testUpdateOrderPriceChanges() throws RemoteException {

		int orderUnits = 20;

		Order one = new Order(TRADER_BUYER_1, SECURITY_1, orderUnits, 20.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis());

		orderbook.buyTrade(one);

		Order two = new Order(TRADER_BUYER_2, SECURITY_1, orderUnits, 20.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis());

		orderbook.buyTrade(two);

		Order oneUpdate = new Order(one.getOrderId(), TRADER_BUYER_1, SECURITY_1, orderUnits/2, 19.0,
				true , OrderType.LIMIT_ORDER, System.currentTimeMillis() + 1000);
		orderbook.update(oneUpdate);


		List<Order> remainingOrders = orderbook.getAllOrders();

		//order one was on the top of orderbook , price of order one was changed hence it lost its preferred position.
		Order topOrder = remainingOrders.iterator().next();
		assertEquals(two, topOrder);

	}

}
