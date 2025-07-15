package contracts

org.springframework.cloud.contract.spec.Contract.make {
    description "Should throw exception when wallet has insufficient balance"
    
    input {
        triggeredBy("triggerInsufficientBalanceEvent()")
    }
    
    output {
        // 當餘額不足時，應該拋出異常，不發送訊息
        assertThat("com.eap.eap_wallet.configuration.ReturnException")
    }
}
