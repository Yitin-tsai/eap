package com.eap.eap_order.controller.dto.req;

import java.util.UUID;

import lombok.Data;

@Data
public class PlaceSellOrderReq {

    private Integer sellPrice;
    private Integer amount;

    private UUID seller;
    
    
}
