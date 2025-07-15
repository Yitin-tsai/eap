package contracts

org.springframework.cloud.contract.spec.Contract.make {
    description "Should throw exception when wallet has insufficient balance"
    
    input {
        messageFrom("order.create.queue")
        messageBody([
            orderId: $(anyUuid()),
            userId: $(anyUuid()),
            price: $(anyPositiveInt()),
            amount: 200, // 超過可用餘額
            orderType: "BUY",
            createdAt: $(anyIso8601WithOffset())
        ])
        messageHeaders {
            messagingContentType("application/json")
        }
    }
    
    output {
        // 當餘額不足時，不應該發送任何訊息到 order.exchange
        // 而是拋出例外
        assertThat("No message should be sent when wallet balance is insufficient")
    }
}
