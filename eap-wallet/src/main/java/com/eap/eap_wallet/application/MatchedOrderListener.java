package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.common.event.OrderMatchedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static com.eap.common.constants.RabbitMQConstants.ORDER_MATCHED_QUEUE;
import static com.eap.common.constants.RabbitMQConstants.WALLET_MATCHED_QUEUE;

@Component
public class MatchedOrderListener {

    @Autowired
    WalletRepository walletRepository;

    @RabbitListener(queues = WALLET_MATCHED_QUEUE)
    public void handleOrderMatched(OrderMatchedEvent event) {
        System.out.println("Received OrderMatchedEvent: " + event);

        // 買方處理：支付成交金額，獲得商品，剩餘鎖定資金解鎖
        WalletEntity buyerWallet = walletRepository.findByUserId(event.getBuyerId());

        // 實際支付金額
        Integer actualPayment = event.getPrice() * event.getAmount();

        // 買方：減少鎖定貨幣（實際支付），增加可用數量
        Integer newBuyerLockedCurrency = buyerWallet.getLockedCurrency() - actualPayment;
        Integer newBuyerAmount = buyerWallet.getAvailableAmount() + event.getAmount();

        buyerWallet.setLockedCurrency(newBuyerLockedCurrency);
        buyerWallet.setAvailableAmount(newBuyerAmount);
        walletRepository.save(buyerWallet);

        // 賣方處理：交出商品，獲得貨幣
        WalletEntity sellerWallet = walletRepository.findByUserId(event.getSellerId());

        // 賣方：減少鎖定數量，增加可用貨幣
        Integer newSellerAmount = sellerWallet.getLockedAmount() - event.getAmount();
        Integer newSellerCurrency = sellerWallet.getAvailableCurrency() + actualPayment;

        sellerWallet.setLockedAmount(newSellerAmount);
        sellerWallet.setAvailableCurrency(newSellerCurrency);
        walletRepository.save(sellerWallet);

        System.out.println("撮合處理完成 - 買方: " + event.getBuyerId() + ", 賣方: " + event.getSellerId() +
                         ", 成交價: " + event.getPrice() + ", 數量: " + event.getAmount() + ", 實際支付: " + actualPayment);
    }
}
