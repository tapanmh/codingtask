package com.example.orderbook.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.orderbook.model.request.OrderRequest;
import com.example.orderbook.model.request.OrderType;
import com.example.orderbook.model.request.UpdateOrderRequest;
import com.example.orderbook.service.OrderBookService;
import com.example.orderbook.service.PriceTimePriorityOrderBook;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@EnableWebMvc
public class OrderBookControllerTest extends AbstractTest {


    @MockBean
    private PriceTimePriorityOrderBook priceTimePriorityOrderBook;

    @MockBean
    private OrderBookService orderBookService;


    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void addOrder() throws Exception {
        String uri = "/addOrder";
        OrderRequest orderRequest = new OrderRequest(Long.valueOf(1), "CLIENT001","TATA",1,100.0,true,OrderType.LIMIT_ORDER);

        String inputJson = super.mapToJson(orderRequest);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void updateOrder() throws Exception {
        String uri = "/addOrder";
        OrderRequest orderRequest = new OrderRequest(Long.valueOf(1), "CLIENT001","TATA",1,100.0,true,OrderType.LIMIT_ORDER);

        String inputJson = super.mapToJson(orderRequest);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);


        uri = "/updateOrder";
        UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest(Long.valueOf(1), 1,100.0);

        inputJson = super.mapToJson(orderRequest);
        mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);

    }


    @Test
    public void cancelOrder() throws Exception {
        String uri = "/addOrder";
        OrderRequest orderRequest = new OrderRequest(Long.valueOf(1), "CLIENT001","TATA",1,100.0,true,OrderType.LIMIT_ORDER);

        String inputJson = super.mapToJson(orderRequest);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);


        uri = "/cancelOrder/{orderId}";
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri,1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);

        assertEquals(mvcResult.getResponse().getContentAsString(), "Order Cancellation request received");

    }

    @Test
    public void getOrders() throws Exception {
        String uri = "/addOrder";
        OrderRequest orderRequest = new OrderRequest(Long.valueOf(1), "CLIENT001","TATA",1,100.0,true,OrderType.LIMIT_ORDER);

        String inputJson = super.mapToJson(orderRequest);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);


        uri = "/getOrders";
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
    }


    @Test
    public void getOrdersBySecurity() throws Exception {
        String uri = "/addOrder";
        OrderRequest orderRequest = new OrderRequest(Long.valueOf(1), "CLIENT001","TATA",1,100.0,true,OrderType.LIMIT_ORDER);

        String inputJson = super.mapToJson(orderRequest);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);


        uri = "/getOrders/{securityId}";
        mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri,"TATA")
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
    }

}