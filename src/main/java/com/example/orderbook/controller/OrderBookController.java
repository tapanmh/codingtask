package com.example.orderbook.controller;

import com.example.orderbook.*;
import com.example.orderbook.exception.TradeException;
import com.example.orderbook.model.Order;
import com.example.orderbook.model.request.OrderRequest;
import com.example.orderbook.model.request.OrderType;
import com.example.orderbook.model.request.UpdateOrderRequest;
import com.example.orderbook.service.OrderBookService;
import com.example.orderbook.service.PriceTimePriorityOrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
public class OrderBookController {


    Logger logger = LoggerFactory.getLogger(OrderBookService.class);

    @Autowired
    PriceTimePriorityOrderBook orders;

    @Autowired
    OrderBookService orderBookService;

    @GetMapping("/getOrders/{securityId}")
    public ResponseEntity getOrdersBySecurity(@PathVariable String securityId) {
        List<Order> orders = orders = orderBookService.listOrdersBySecurity(securityId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }


    @GetMapping("/getOrders")
    public ResponseEntity getOrders() {
        List<Order> orders = orders = orderBookService.listOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PostMapping("/addOrder")
    public ResponseEntity addOrder(@Valid @RequestBody OrderRequest orderRequest){
        String response = null;

        if(orderRequest.getOrderType().equals(OrderType.LIMIT_ORDER)){
            if(orderRequest.getValue() == null || orderRequest.getValue() <= 0 ){
                throw new TradeException("Value is mandatory for LIMIT ORDERs and must be greater than zero ");
            }
        }
        response = orderBookService.bookOrder(orderRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/updateOrder")
    public ResponseEntity updateOrder(@Valid @RequestBody UpdateOrderRequest orderRequest){
        String response = orderBookService.updateOrder(orderRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/cancelOrder/{orderId}")
    public ResponseEntity cancelOrder(@PathVariable(required = true) Long orderId) {
        orderBookService.cancelOrder(orderId);
        return new ResponseEntity<>("Order Cancellation request received", HttpStatus.OK);
    }

}
