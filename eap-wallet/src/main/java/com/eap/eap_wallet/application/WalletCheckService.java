package com.eap.eap_wallet.application;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class WalletCheckService {

    @Autowired
    WalletRepository walletRepository;


    public boolean checkWallet(OrderCreatedEvent event) {
        if (!isWalletEnough(event)) {
            log.warn("訂單金額超過可用餘額: " + event.getUserId());
            return false;
        }

        if (!isWalletEnoughForSell(event)) {
            log.warn("訂單可用電量不足: " + event.getUserId());
            return false;
        }

        lockAsset(event);
        return true;
    }


    private boolean isWalletEnough(OrderCreatedEvent event) {

        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());
        if (wallet == null) {
            log.warn("找不到使用者錢包: " + event.getUserId());
            return false;
        }
        if (event.getOrderType() == "BUY" && event.getAmount() * event.getPrice() > wallet.getAvailableCurrency()) {
            log.warn("訂單總金額超過可用餘額: " + event.getUserId());
            return false;
        }

        return true;
    }

    private boolean isWalletEnoughForSell(OrderCreatedEvent event) {

        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());
        if (wallet == null) {
            log.warn("找不到使用者錢包: " + event.getUserId());
            return false;

        }
        if (event.getOrderType() == "SELL" && event.getAmount() > wallet.getAvailableAmount()) {
            log.warn("訂單總電量超過可供應電量: " + event.getUserId());
            return false;

        }
        return true;
    }

    private void lockAsset(OrderCreatedEvent event) {
        WalletEntity wallet = walletRepository.findByUserId(event.getUserId());

        if ("BUY".equals(event.getOrderType())) {
            int lockCurrency = event.getPrice() * event.getAmount();
            wallet.setAvailableCurrency(wallet.getAvailableCurrency() - lockCurrency);
            wallet.setLockedCurrency(wallet.getLockedCurrency() + lockCurrency);
        } else if ("SELL".equals(event.getOrderType())) {
            int lockAmount = event.getAmount();
            wallet.setAvailableAmount(wallet.getAvailableAmount() - lockAmount);
            wallet.setLockedAmount(wallet.getLockedAmount() + lockAmount);
        }

        walletRepository.save(wallet);
        log.info("🔒 資產鎖定完成，用戶: {}", event.getUserId());
    }
}
