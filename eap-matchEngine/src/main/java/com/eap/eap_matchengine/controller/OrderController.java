package com.eap.eap_matchengine.controller;

import com.eap.common.event.OrderCancelEvent;
import com.eap.eap_matchengine.application.OrderCancelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("v1/order")
public class OrderController {
    @Autowired
    OrderCancelService orderCancelService;

    @DeleteMapping("cancel")
    public boolean cancelOrder(OrderCancelEvent event) {
    return  orderCancelService.execute(event);
    }
}
