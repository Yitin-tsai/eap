package contracts

org.springframework.cloud.contract.spec.Contract.make {
    description "Should process order create event successfully when wallet has sufficient balance"
    
    input {
        triggeredBy("triggerOrderCreateEvent()")
    }
    
    output {
        sentTo("order.exchange")
        messageBody([
            orderId: "87654321-4321-4321-4321-210987654321",
            userId: "12345678-1234-1234-1234-123456789012",
            price: 100,
            quantity: 50,
            type: "BUY",
            createdAt: $(anyIso8601WithOffset())
        ])
        messageHeaders {
            messagingContentType("application/json")
        }
    }
}
