package com.eap.common.constants;

/**
 * RabbitMQ 相關常量配置
 * 集中管理所有 Queue、Exchange 和 Routing Key 的名稱
 */
public class RabbitMQConstants {
    
    // Queue 名稱
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_MATCHED_QUEUE = "order.matched.queue";
    public static final String WALLET_MATCHED_QUEUE = "wallet.matched.queue";
    
    // Exchange 名稱
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    // Routing Key
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_MATCHED_KEY = "order.matched";
    public static final String WALLET_MATCHED_KEY = "wallet.matched";
    
    private RabbitMQConstants() {
        // 私有構造函數防止實例化
    }
}
