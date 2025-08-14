package com.eap.eap_order.application.OutBound;


import com.eap.common.event.OrderCancelEvent;
import com.eap.common.event.OrderCreatedEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "eap-matchEngine", url = "${eap.matchEngine.base-url}")
public interface EapMatchEngine {

    @DeleteMapping("/v1/order/cancel")
    public boolean cancelOrder(OrderCancelEvent event);

    @GetMapping("/v1/order/query")
    public ResponseEntity<List<OrderCreatedEvent>> queryOrder(String userId);
}
