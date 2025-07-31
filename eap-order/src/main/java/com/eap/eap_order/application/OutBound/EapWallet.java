package com.eap.eap_order.application.OutBound;


import com.eap.common.event.OrderCreateEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "eap-wallet", url = "${eap.wallet.url}")
@RequestMapping("/v1/wallet")
public interface EapWallet {

    @PostMapping("/check")
    public boolean checkWallet(OrderCreateEvent event) ;
}
