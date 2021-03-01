# Price-Time priority order matching engine

##Features
- Order matching engine is written in Java using spring boot
- API's are provided to 
    - place an order
    - update an order
    - cancel an order
    - list all orders
    - list orders by securityId
- Tests are available in test folder
- Internal processing of Order Matching engine is logged on console to display order matching, updation, cancellation and transaction values etc.

## Requirements
* Design and implement a price-time priority order matching engine in Java. : DONE
* an order book that keeps track of all bid (buy) and ask (sell) orders. : DONE
    - Order Matching is handled in PriceTimePriorityOrderBook.java
* There is exactly one order book per security (stock) : DONE
* Each order is characterized by transaction type (Buy or Sell),
 order type (Market, Limit), quantity, price (for Limit orders) and entry timestamp : DONE
    - Please refer the object OrderRequest.java
* Market orders are instructions to buy / sell at best possible price : DONE
    - API to place the order accepts LIMIT_ORDER and MARKET_ORDER
    - Order Matching is handled in PriceTimePriorityOrderBook.java
* Limit orders are instructions to buy at a predefined price x or lower, or to sell at a predefined price x or higher: DONE
    - Order Matching is handled in PriceTimePriorityOrderBook.java
* The bids are sorted in descending order and asks are sorted in ascending order in terms of price.
 Hence, the top row of an order book always shows orders with the best price: the highest bid and the lowest ask - DONE
    - Place order with the API (for placing an order)
    - Use API to list orders to verify if the orders are sorted in ascending/descending order
* In every matching cycle orders in an order book get ‘matched’ / executed. 
First matching/execution priority is price, i.e. orders with the best price get matched first. 
If two orders have the same price, execution priority is given to the first arrived order  : DONE
    - API to place order is available
    - API to list orders is available to check the status of orders, this can help verify if the order is PLACED/MATCHED/UPDATED/CANCELLED etc.
    - important operations are logged on console to display MATCHed orders
* Order book should support the following operations: NEW ORDER, AMEND ORDER, CANCEL ORDER, MATCH. : DONE
    - API's are available for all the above operations
* the system should support querying the current state of an order book at any given time. : DONE
    - API's to list all orders in order book and all orders per security are available 
  

## API details
**please refer resources folder for sample JSON requests**

####API to place an order
- POST http://localhost:8080/addOrder
- Sample request for a sale order
<pre>
{
	"clientId" : "CLIENT001",
	"securityId" : "REL",
	"units" :  100,
	"value" : 80,
	"isBuying" : false,
	"orderType" : "LIMIT_ORDER"
}


- valid values for orderType is LIMIT_ORDER and MARKET_ORDER
- valid values for isBuying is true and false, this field is for specifying bid (buy) and ask (sell) orders
    - true for bid (buy)
    - false for ask (sell) orders 
</pre>

###API to update an order
- POST : http://localhost:8080/updateOrder
<pre>
{
	"orderId": 0,
	"units": 102,
	"value": 90
}

- orderId is mandatory for updating any pre-existing order
</pre> 
**please refer resources folder for other API's**


### Run the application
- command to run the application is: mvn spring-boot:run 
    - application will run on port 8080

### Run the test
- command to run the tests is: mvn test
- test result is
<pre>
[INFO] Results:
[INFO]
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO]
[INFO] --- jacoco-maven-plugin:0.8.2:report (report) @ order-booking-service ---
[INFO] Loading execution data file G:\Installer\codingtask\target\jacoco.exec
[INFO] Analyzed bundle 'order-booking-service' with 14 classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  11.857 s
[INFO] Finished at: 2021-03-01T15:40:29+05:30
[INFO] ------------------------------------------------------------------------
</pre>

## Assumption
- Tying to match a market order with another market order, what will be the unit price for executing the trade?
(reason - client placed a market order without specifying price at which it has to be exeucted
then order engine will calculate the best available price using the unit price of other limit orders 
available in order book): 
 
  If a client places an market order whereas previously placed matching orders from other clients
 are queued/available in the orderbook, in such case if the queued order are also market orders 
 then order engine will match the best available queued market order but security unit price will be
 taken from the best available limit order
 <pre>
 For ex: 
 CLIENT001 places a BUY market order for 100 units
 
 Queued SELL orders available in order book are :
 Order no| Units | Price   | Order_Type
    1       100     -        MARKET_ORDER
    2       100     -        MARKET_ORDER
    3       100    110 $     LIMIT_ORDER 

    
While Matched SELL order from orderbook will be order no#1 (because it is on the top)
BUT unit price will be taken as 110$ for trade execution (order#3)    
</pre>