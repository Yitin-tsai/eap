package com.eap.eap_order.controller;

import com.eap.eap_order.application.PlaceBuyOrderService;
import com.eap.eap_order.application.PlaceSellOrderService;
// 引入你設計的 DTO 類別
import com.eap.eap_order.controller.dto.req.PlaceBuyOrderReq;
import com.eap.eap_order.controller.dto.req.PlaceSellOrderReq;
import com.eap.eap_order.controller.dto.res.ListBuyOrderRes;
import com.eap.eap_order.controller.dto.res.ListSellOrderRes;
import com.eap.eap_order.controller.dto.res.ListUserOrderRes;
import com.eap.eap_order.controller.dto.res.MatchHistoryRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bid")
@Validated
@Tag(name = "bid", description = "競價系統 API")
@Slf4j
public class OrderController {

  protected PlaceBuyOrderService placeBuyOrderService;

  protected PlaceSellOrderService placeSellOrderService;

  @Operation(operationId = "post-bid-add", summary = "掛買單", description = "掛單功能 - 買入訂單")
  @ApiResponse(responseCode = "200", description = "掛單成功")
  @ApiResponse(responseCode = "400", description = "請求錯誤")
  @PostMapping("/buy")
  public ResponseEntity<Void> postBidAdd(
      @Parameter(description = "驗證用戶登入") @RequestHeader(value = "ID_TOKEN", required = false)
          String idToken,
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq,
      @Parameter(description = "掛買單請求") @Valid @RequestBody PlaceBuyOrderReq request) {

    log.info("掛買單請求: {}", request);
    placeBuyOrderService.execute(request);
    return ResponseEntity.ok().build();
  }

  @Operation(operationId = "get-bid-buy", summary = "查詢買單列表", description = "取得目前所有買入訂單")
  @ApiResponse(responseCode = "200", description = "查詢成功")
  @ApiResponse(responseCode = "400", description = "請求錯誤")
  @GetMapping("/buy")
  public ResponseEntity<ListBuyOrderRes> getBidBuy(
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq) {

    log.info("查詢買單列表，txnSeq: {}", txnSeq);

    // 暫時回傳空的資料結構
    ListBuyOrderRes response = new ListBuyOrderRes();
    return ResponseEntity.ok(response);
  }

  @Operation(operationId = "post-bid-sell", summary = "掛賣單", description = "掛單功能 - 賣出訂單")
  @ApiResponse(responseCode = "200", description = "掛單成功")
  @ApiResponse(responseCode = "400", description = "請求錯誤")
  @PostMapping("/sell")
  public ResponseEntity<Void> postBidSell(
      @Parameter(description = "驗證用戶登入") @RequestHeader(value = "ID_TOKEN", required = false)
          String idToken,
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq,
      @Parameter(description = "掛賣單請求") @Valid @RequestBody PlaceSellOrderReq request) {

    log.info("掛賣單請求: {}", request);

    placeSellOrderService.placeSellOrder(request);
    return ResponseEntity.ok().build();
  }

  @Operation(operationId = "get-bid-sell", summary = "查詢賣單列表", description = "取得目前所有賣出訂單")
  @ApiResponse(responseCode = "200", description = "查詢成功")
  @ApiResponse(responseCode = "400", description = "請求錯誤")
  @GetMapping("/sell")
  public ResponseEntity<ListSellOrderRes> getBidSell(
      @Parameter(description = "驗證用戶登入") @RequestHeader(value = "ID_TOKEN", required = false)
          String idToken,
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq) {

    log.info("查詢賣單列表，idToken: {}, txnSeq: {}", idToken, txnSeq);

    // 暫時回傳空的資料結構
    ListSellOrderRes response = new ListSellOrderRes();
    return ResponseEntity.ok(response);
  }

  @Operation(operationId = "get-bid-user-order", summary = "查詢用戶訂單", description = "取得特定用戶的所有訂單")
  @ApiResponse(responseCode = "200", description = "查詢成功")
  @GetMapping("/user-order")
  public ResponseEntity<ListUserOrderRes> getBidUserOrder(
      @Parameter(description = "驗證用戶登入") @RequestHeader(value = "ID_TOKEN", required = false)
          String idToken,
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq) {

    log.info("查詢用戶訂單，idToken: {}, txnSeq: {}", idToken, txnSeq);

    // 暫時回傳空的資料結構
    ListUserOrderRes response = new ListUserOrderRes();
    return ResponseEntity.ok(response);
  }

  @Operation(operationId = "get-bid-match-history", summary = "查詢撮合歷史", description = "取得撮合成交記錄")
  @ApiResponse(responseCode = "200", description = "查詢成功")
  @ApiResponse(responseCode = "400", description = "請求錯誤")
  @GetMapping("/match-history")
  public ResponseEntity<MatchHistoryRes> getBidMatchHistory(
      @Parameter(description = "交易編號") @RequestHeader(value = "txnSEq", required = false)
          String txnSeq) {

    log.info("查詢撮合歷史，txnSeq: {}", txnSeq);

    // 暫時回傳空的資料結構
    MatchHistoryRes response = new MatchHistoryRes();
    return ResponseEntity.ok(response);
  }
}
