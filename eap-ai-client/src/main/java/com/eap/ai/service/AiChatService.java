package com.eap.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;



@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;

        private static final String SYSTEM_PROMPT = """
                        你是 EAP 電力交易平台的輔助工具。當需要查詢或執行工具時，請直接回傳該工具的執行結果文字（或可以被人類閱讀的結構化文字）。

                        指引：
                        - 直接回傳工具結果（例如：訂單簿、模擬報告、錯誤訊息），不要僅回傳 JSON 計畫。
                        - 若需要呼叫 runSimulation 或 exportReport，請在回覆中說明要呼叫的工具與必要參數，但最終直接回傳該工具的結果。
                        - 若無法提供有用回覆，請直接回傳一個簡潔的錯誤字串。

                        可用工具（參考）： getOrderBook, getMarketMetrics, placeOrder, cancelOrder, getUserOrders, registerUser, runSimulation, exportReport
                        """;

    // ===== 公開入口：單輪規劃 → 執行 → 回傳結果 =====
    public String chat(String userMessage) {
        try {
            log.info("收到用戶訊息: {}", userMessage);

            String prompt = SYSTEM_PROMPT + "\n使用者提問：" + userMessage;
            String modelOut = chatClient.prompt(prompt).call().content();

            // 直接回傳 LLM 的文字輸出
            return modelOut == null ? "錯誤：未取得模型回應" : modelOut;

        } catch (Exception e) {
            log.error("處理失敗", e);
            return "處理請求時發生錯誤：" + e.getMessage();
        }
    }

    // plan parsing/execution removed: this service will forward LLM output directly

    /**
     * 簡單的系統狀態摘要，供 CLI / controller 顯示用。
     * 這個方法不應該執行耗時或會拋例外的操作。
     */
    public String getSystemStatus() {
        try {
            return "chatClient=" + (chatClient == null ? "missing" : "ok");
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
