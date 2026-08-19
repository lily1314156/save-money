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

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/change-password")
    public String changePassword() {
        return "change-password";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }
}