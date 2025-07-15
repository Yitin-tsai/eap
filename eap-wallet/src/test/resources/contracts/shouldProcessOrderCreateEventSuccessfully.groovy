package contracts

org.springframework.cloud.contract.spec.Contract.make {
    description "Should process order create event successfully when wallet has sufficient balance"
    
    input {
        messageFrom("order.create.queue")
        messageBody([
            orderId: $(anyUuid()),
            userId: $(anyUuid()),
            price: $(anyPositiveInt()),
            amount: 50,
            orderType: $(regex("[A-Z]+")),
            createdAt: $(anyIso8601WithOffset())
        ])
        messageHeaders {
            messagingContentType("application/json")
        }
    }
    
    output {
        sentTo("order.exchange")
        messageBody([
            orderId: $(fromRequest().body('$.orderId')),
            userId: $(fromRequest().body('$.userId')),
            price: $(fromRequest().body('$.price')),
            quantity: $(fromRequest().body('$.amount')),
            type: $(fromRequest().body('$.orderType')),
            createdAt: $(fromRequest().body('$.createdAt'))
        ])
        messageHeaders {
            messagingContentType("application/json")
        }
    }
}
