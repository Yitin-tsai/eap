package com.eap.eap_wallet.application;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

@SpringBootTest
@AutoConfigureMessageVerifier
public abstract class CreateOrderListenerContractBase {

    @BeforeEach
    public void setup() {
        // 可在這裡初始化測試資料庫或 mock
    }
}
