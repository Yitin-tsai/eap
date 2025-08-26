package com.eap.eap_order.application;

import com.eap.eap_order.configuration.repository.MathedOrderRepository;
import com.eap.eap_order.domain.entity.MatchOrderEntity;
import com.eap.common.event.OrderMatchedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.eap.common.constants.RabbitMQConstants.*;

@Component
public class MatchEventListener {

    @Autowired
    private MathedOrderRepository matchOrderRepository;
    
    @Autowired
    private MarketDataService marketDataService;

    @RabbitListener(queues = ORDER_MATCHED_QUEUE)
    public void handleOrderMatched(OrderMatchedEvent event) {
        System.out.println("Received OrderMatchedEvent: " + event);

        MatchOrderEntity matchOrder =
                MatchOrderEntity.builder()
                        .buyerUuid(event.getBuyerId())
                        .sellerUuid(event.getSellerId())
                        .price(event.getDealPrice())
                        .amount(event.getAmount())
                        .updateTime(event.getMatchedAt())
                        .orderType(event.getOrderType())
                        .build();

        // 保存成交記錄
        MatchOrderEntity savedOrder = matchOrderRepository.save(matchOrder);
        
        // 推送實時成交數據到 WebSocket
        marketDataService.pushRealtimeTrade(savedOrder);
        
        // 推送更新後的市場統計數據
        marketDataService.pushMarketData();
    }
}
