package com.eap.eap_wallet.controller;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_wallet.application.WalletCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wallet")
public class WalletController {


    @Autowired
    WalletCheckService walletCheckService;

    @PostMapping("/check")
    public boolean checkWallet(@RequestBody OrderCreatedEvent event) {
        return walletCheckService.checkWallet(event);
    }
}