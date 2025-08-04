package com.eap.eap_order.application.OutBound;


import com.eap.common.event.OrderCreatedEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "eap-wallet", url = "${eap.wallet.base-url}")
public interface EapWallet {

    @PostMapping("/v1/wallet/check")
    public boolean checkWallet(OrderCreatedEvent event) ;
}
