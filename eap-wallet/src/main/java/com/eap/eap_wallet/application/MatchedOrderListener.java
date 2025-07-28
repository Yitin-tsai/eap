package com.eap.eap_wallet.application;

import com.eap.eap_wallet.configuration.repository.WalletRepository;
import com.eap.eap_wallet.domain.entity.WalletEntity;
import com.eap.common.event.OrderMatchedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;

import static com.eap.common.constants.RabbitMQConstants.ORDER_MATCHED_QUEUE;
import static com.eap.common.constants.RabbitMQConstants.WALLET_MATCHED_QUEUE;

public class MatchedOrderListener {

    @Autowired
    WalletRepository walletRepository;

    @RabbitListener(queues = WALLET_MATCHED_QUEUE)
    public void handleOrderMatched(OrderMatchedEvent event) {
        System.out.println("Received OrderMatchedEvent: " + event);

        WalletEntity buyerWallet = walletRepository.findByUserId(event.getBuyerId());

        Integer newBuyerBalance = buyerWallet.getLockedCurrency() - (event.getPrice() * event.getAmount());
        buyerWallet.setLockedCurrency(newBuyerBalance);
        walletRepository.save(buyerWallet);

        WalletEntity sellerWallet = walletRepository.findByUserId(event.getSellerId());

        Integer newSellerAmount = sellerWallet.getLockedAmount() - event.getAmount();
        Integer newSellerBalance = sellerWallet.getAvailableCurrency() + (event.getPrice() * event.getAmount());
        sellerWallet.setLockedAmount(newSellerAmount);
        sellerWallet.setAvailableCurrency(newSellerBalance);
        walletRepository.save(sellerWallet);


    }
}
