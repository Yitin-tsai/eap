package com.eap.eap_order.controller.dto.req;

import lombok.Data;

@Data
public class PlaceSellOrderReq {

    private Integer sellPrice;
    private Integer amount;

    private String seller;
    
    private String updateTime; 
    
}
