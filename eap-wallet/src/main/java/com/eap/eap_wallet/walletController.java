package com.eap.eap_wallet;

import com.eap.common.event.OrderCreateEvent;
import com.eap.eap_wallet.application.WalletCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wallet")
public class walletController {


    @Autowired
    WalletCheckService walletCheckService;

    @PostMapping("/check")
    public boolean checkWallet(@RequestBody OrderCreateEvent event) {
        return walletCheckService.checkWallet(event);
    }
}