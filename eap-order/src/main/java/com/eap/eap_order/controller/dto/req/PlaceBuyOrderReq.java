package com.eap.eap_order.controller.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceBuyOrderReq {

  @NotNull private Integer bidPrice;
  @NotNull private Integer amount;
  @NotNull private String bidder;
}
