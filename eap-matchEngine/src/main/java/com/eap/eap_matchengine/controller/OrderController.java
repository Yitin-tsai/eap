package com.eap.eap_matchengine.controller;

import com.eap.common.event.OrderCancelEvent;
import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_matchengine.application.OrderCancelService;
import com.eap.eap_matchengine.application.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("v1/order")
public class OrderController {
    @Autowired
    OrderCancelService orderCancelService;

    @Autowired
    OrderQueryService orderQueryService;

    @DeleteMapping("cancel")
    public boolean cancelOrder(@RequestBody OrderCancelEvent event) {
    return  orderCancelService.execute(event);
    }

    @GetMapping("query/{userId}")
    public ResponseEntity<List<OrderCreatedEvent>> queryOrder(@PathVariable UUID userId) {
        List<OrderCreatedEvent> orders = orderQueryService.excute(userId);
        return ResponseEntity.ok(orders);
    }
}
