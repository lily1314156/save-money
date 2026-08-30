package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 後端呼叫 Google Maps 相關 API 的服務。
 * 方法：
 *   - geocode(address)  地址 → 經緯度
 */
@Service
public class GoogleMapService {

    // 後端 key
    @Value("${google.maps.api-server-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // 把 response body（String）轉成 JsonNode 用
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 共用的 GET 請求方法。
     *   - 傳入：要打的 URI、API 名稱（錯誤訊息用）
     *   - 回傳：JSON 解析後的 JsonNode
     * 把「發送 + 檢查狀態碼 + 解析 JSON」三件事包在一起，
     * 各 API 方法只要呼叫 send() 就好，不用每次重複寫。
     */
    private JsonNode send(URI uri, String apiName) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            // BodyHandlers.ofString() = 把回應 body 當成字串拿
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        apiName + " HTTP 錯誤：statusCode=" + response.statusCode());
            }
            // 字串 → JsonNode
            return objectMapper.readTree(response.body());

        } catch (IOException e) {
            // 網路、解析 JSON 失敗
            throw new IllegalStateException(apiName + " 呼叫失敗：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            // 被中斷要把 interrupt 旗標補回去，不然會被吞掉
            Thread.currentThread().interrupt();
            throw new IllegalStateException(apiName + " 被中斷", e);
        }
    }

    @PostConstruct
    public void checkApiKeyLoaded() {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠ GOOGLE_MAPS_API_KEY 沒讀到！檢查環境變數有沒有設。");
            return;
        }
        System.out.println("✅ Google API key 已載入");
    }

    /**
     * 地址 → 經緯度。
     *
     * 回傳值用 Optional 包：
     *   - 找得到（status=OK）→ Optional.of(GeoLocation)
     *   - 地址打錯找不到（status=ZERO_RESULTS）→ Optional.empty()
     *   - 其他錯（key 壞、超量、IP 不對）→ 直接丟例外，讓呼叫端決定怎麼處理
     */
    public Optional<GeoLocation> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        // 用 UriComponentsBuilder 幫你正確 URL-encode 中文
        URI uri = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("address", address) // 先暫存
                .queryParam("key", apiKey)
                .queryParam("language", "zh-TW")
                .queryParam("region", "tw")
                .build()
                .encode()  // ← 關鍵：把中文轉成 %E5%8F%B0... 這種 URL 安全格式
                .toUri();

        // 一行打 API、檢查狀態、解析 JSON
        JsonNode geocodeNode = send(uri, "Google Geocoding API");

        // 看 Google 回的 status 決定怎麼處理
        String status = geocodeNode.path("status").asText();
        switch (status) {
            case "OK":
                // results[0].geometry.location.{lat,lng}
                JsonNode location = geocodeNode.path("results").get(0)
                        .path("geometry").path("location");
                return Optional.of(new GeoLocation(
                        location.path("lat").asDouble(),
                        location.path("lng").asDouble()
                ));

            // 地址 Google 找不到，這不算錯誤，回空 list
            case "ZERO_RESULTS":
                return Optional.empty();

            // key 或 IP 不對 / OVER_QUERY_LIMIT（超量）/ INVALID_REQUEST 等
            default:
                String errorMessage = geocodeNode.path("error_message").asText("(無詳細訊息)");
                throw new IllegalStateException(
                        "Geocoding API 錯誤：status=" + status + ", message=" + errorMessage);
        }
    }

    // 經緯度結果的小型容器。用 record 寫一行搞定，自動有 getter、equals、toString。
    public record GeoLocation(double lat, double lng) {

    }

    /**
     * 直接用品牌名當查詢字串，再限縮地點範圍
     * placeType：Places 的類別過濾（例：convenience_store），
     * 讓 Google 端就只回這個類別的店
     * 傳 null = 不過濾
     */
    public List<PlaceResult> textSearchBrand(double lat, double lng, String query,
                                             int radiusMeters, String placeType) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/place/textsearch/json")
                // query 會直接比對店名
                .queryParam("query",    query)          //品牌名稱「星巴克」、「50嵐」
                .queryParam("location", lat + "," + lng)    //中心點緯度 + 經度
                .queryParam("radius",   radiusMeters)       //範圍半徑，單位公尺
                .queryParam("language", "zh-TW")
                .queryParam("key",      apiKey);

        // 有設 type 才加這個參數
        if (placeType != null && !placeType.isBlank()) {
            builder.queryParam("type", placeType);
        }

        URI uri = builder.build().encode().toUri();

        JsonNode textSearchNode = send(uri, "Places Text Search API");

        String status = textSearchNode.path("status").asText();
        if ("ZERO_RESULTS".equals(status)) {
            return List.of();
        }
        if (!"OK".equals(status)) {
            String errorMessage = textSearchNode.path("error_message").asText("(無詳細訊息)");
            throw new IllegalStateException(
                    "Places Text Search API 錯誤：status=" + status + ", message=" + errorMessage);
        }

        List<PlaceResult> results = new ArrayList<>();
        for (JsonNode item : textSearchNode.path("results")) {
            JsonNode loc = item.path("geometry").path("location");
            results.add(new PlaceResult(
                    item.path("place_id").asText(),
                    item.path("name").asText(),

                    // formatted_address（完整地址）
                    item.path("formatted_address").asText(),
                    loc.path("lat").asDouble(),
                    loc.path("lng").asDouble()
            ));
        }
        return results;
    }

    public record PlaceResult(
            String placeId,
            String name,
            String address,
            double lat,
            double lng
    ) {}

    // ──────────────────────────────────────────────
    // Place Details：用 place_id 補抓電話 + 營業時間。
    // 所以把 Places 店家寫進 DB 前要多打一次這支。
    // fields 只填要用的兩個欄位（依欄位計費）。
    // ──────────────────────────────────────────────
    public Optional<PlaceDetails> placeDetails(String placeId) {
        if (placeId == null || placeId.isBlank()) return Optional.empty();

        URI uri = UriComponentsBuilder
                .fromUriString("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields",   "formatted_phone_number,opening_hours")
                .queryParam("language", "zh-TW")
                .queryParam("key",      apiKey)
                .build()
                .encode()
                .toUri();

        JsonNode detailsNode = send(uri, "Place Details API");

        if (!"OK".equals(detailsNode.path("status").asText())) {
            // 拿不到就回空，讓呼叫端存 null（顯示「—」），不要讓整批搜尋炸掉
            return Optional.empty();
        }

        JsonNode result = detailsNode.path("result");

        // asText(null)：欄位不存在時回 null，而不是空字串
        String phone = result.path("formatted_phone_number").asText(null);

        // weekday_text 是七行：["星期一: 11:00 – 21:30", "星期二: ...", ...]
        // DB 的 business_hours 只有 VARCHAR(100)，塞不下七行，策略：
        //   七天時段都一樣（連鎖店最常見）→ 只存時段，例：「11:00 – 21:30」
        //   各天不一樣 → 存今天那一行，例：「星期五: 11:00 – 21:30」
        String hours = null;
        JsonNode weekdayText = result.path("opening_hours").path("weekday_text");
        if (weekdayText.isArray() && weekdayText.size() > 0) {
            Set<String> distinctTimes = new HashSet<>();
            for (JsonNode line : weekdayText) {
                distinctTimes.add(timePart(line.asText()));
            }
            if (distinctTimes.size() == 1) {
                hours = distinctTimes.iterator().next();
            } else {
                // DayOfWeek：週一=1 … 週日=7；weekday_text 從星期一開始 → 減 1 當索引
                int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
                if (todayIdx < weekdayText.size()) {
                    hours = weekdayText.get(todayIdx).asText();
                }
            }
            // 保險：超過欄位長度就截斷
            if (hours != null && hours.length() > 100) {
                hours = hours.substring(0, 100);
            }
        }

        return Optional.of(new PlaceDetails(phone, hours));
    }

    /** 從「星期一: 11:00 – 21:30」取出冒號後的時段部分 */
    private static String timePart(String weekdayLine) {
        if (weekdayLine == null) return "";
        // 同時處理半形冒號和全形冒號
        String[] parts = weekdayLine.split("\\s*[:：]\\s*", 2);
        return parts.length == 2 ? parts[1].trim() : weekdayLine.trim();
    }

    // 電話 + 營業時間的小容器
    public record PlaceDetails(String phone, String businessHours) {}
}