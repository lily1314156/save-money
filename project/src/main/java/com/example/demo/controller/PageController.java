package com.example.demo.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import com.example.demo.dao.UsersDao;
import com.example.demo.entity.Users;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final UsersDao usersDao;

    /**
     * 給前端用的 Google Maps key（會印進 HTML，和後端那把不同）。
     * 值的來源鏈：環境變數 GOOGLE_MAPS_BROWSER_KEY
     *   → application.properties 的 google.maps.platform-key=${GOOGLE_MAPS_BROWSER_KEY:}
     *   → 這個欄位
     * 注意這裡「不能」加 final。這個類別有 @RequiredArgsConstructor，
     * final 欄位會被 Lombok 塞進建構子，但 Lombok 預設不會把 @Value 一起搬過去，
     * Spring 就找不到值可以注入了。非 final 才會走欄位注入。
     */
    @Value("${google.maps.platform-key}")
    private String googleMapsApiKey;

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "index";
    }

    @GetMapping("/stores")
    public String stores() {
        return "stores";
    }

    /**
     * 會員頁。
     * loginUser 是 AuthInterceptor 驗證 token 後掛上去的，但它來自 Redis，
     * 是「登入當下」的快照；這裡再用 id 去 DB 查一次，確保畫面顯示的是最新資料。
     * （查不到就退回用快照，避免整頁掛掉）
     */
    @GetMapping("/profile")
    public String profile(@RequestAttribute("loginUser") Users loginUser, Model model) {
        Users user = usersDao.selectById(loginUser.getId());
        model.addAttribute("user", user != null ? user : loginUser);
        return "profile";
    }

    @GetMapping("/change-password")
    public String changePassword() {
        return "change-password";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }
}