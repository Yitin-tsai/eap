package com.eap.eap_order.application;

import com.eap.common.event.OrderCreatedEvent;
import com.eap.common.event.OrderMatchedEvent;
import com.eap.common.event.OrderFailedEvent;
import com.eap.eap_order.controller.OrderStatusController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import static com.eap.common.constants.RabbitMQConstants.*;

@Component
@Slf4j
public class OrderStatusUpdateListener {

    @Autowired
    private OrderStatusController orderStatusController;

    /**
     * 監聽OrderCreatedEvent - 表示wallet檢查通過，訂單已進入撮合
     */
    @RabbitListener(queues = ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到OrderCreatedEvent，更新訂單狀態: {}", event.getOrderId());
        orderStatusController.updateOrderStatus(
            event.getOrderId(),
            "WALLET_CHECK_PASSED",
            "餘額檢查通過，已進入撮合佇列"
        );
    }

    /**
     * 監聽OrderMatchedEvent - 表示撮合成功
     */
    @RabbitListener(queues = ORDER_MATCHED_QUEUE)
    public void onOrderMatched(OrderMatchedEvent event) {
        log.info("收到OrderMatchedEvent，訂單撮合成功: buyerId={}, sellerId={}",
                event.getBuyerId(), event.getSellerId());

        // 這裡需要根據event中的訂單信息來更新狀態
        // 暫時先記錄日誌
        log.info("撮合成功 - 價格: {}, 數量: {}", event.getDealPrice(), event.getAmount());
    }

    /**
     * 監聽OrderFailedEvent - 表示訂單處理失敗（如餘額不足）
     */
    @RabbitListener(queues = ORDER_FAILED_QUEUE)
    public void onOrderFailed(OrderFailedEvent failedEvent) {
        log.info("收到訂單失敗通知: {} - {} ({})",
                failedEvent.getOrderId(), failedEvent.getReason(), failedEvent.getFailureType());

        String status = "INSUFFICIENT_BALANCE".equals(failedEvent.getFailureType()) ?
                       "INSUFFICIENT_BALANCE" : "FAILED";

        orderStatusController.updateOrderStatus(
            failedEvent.getOrderId(),
            status,
            failedEvent.getReason()
        );
    }
}
