package com.eap.eap_order.controller.dto.res;

import java.util.List;

import lombok.Data;

 @Data
public class ListUserOrderRes {
    List<userorder> userOrders;

    @Data
    public class userorder {
        private Integer Price;
        private Integer amount;

        private String type;

        private String updateTime;

    }
    
}
