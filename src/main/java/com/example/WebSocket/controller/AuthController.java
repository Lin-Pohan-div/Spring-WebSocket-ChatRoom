package com.example.WebSocket.controller;

import com.example.WebSocket.dto.Login;
import com.example.WebSocket.entity.Member;
import com.example.WebSocket.repo.MemberRepo;
import com.example.WebSocket.response.LoginResponse;
import com.example.WebSocket.util.JwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RequestMapping("/auth")
@RestController
public class AuthController {

    @Autowired
    private MemberRepo memberRepo;

    @Autowired // 處理加鹽
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Login login) {
        if (memberRepo.findByEmail(login.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email 已被註冊");
        }
        Member member = new Member();
        member.setEmail(login.getEmail());
        member.setPassword(passwordEncoder.encode(login.getPassword()));
        member.setName(login.getEmail());
        memberRepo.save(member);
        return ResponseEntity.ok("註冊成功");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        Optional<Member> member = memberRepo.findByEmail(login.getEmail());

        // 1. 檢查帳號是否存在
        if (member.isEmpty()) {
            System.out.println("❌ 登入失敗：找不到 Email");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號錯誤");
        }

        // 2. 使用 PasswordEncoder 比對加密密碼
        if (!passwordEncoder.matches(login.getPassword(), member.get().getPassword())) {
            System.out.println("❌ 登入失敗：密碼錯誤");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("密碼錯誤");
        }

        // 3. 登入成功：產生 Token
        // 這裡調用你剛才提供的 JwtToken.createToken 方法
        String token = JwtToken.createToken(member.get().getEmail());

        // 4. 封裝回傳資料 (對應你前端需要的 data.token 和 data.email)
        LoginResponse response = new LoginResponse();
        response.setEmail(member.get().getEmail());
        response.setToken(token);
        // 如果 LoginResponse 還有 name 欄位，也可以加上：
        // response.setName(member.get().getName());

        System.out.println("✅ 登入成功：" + login.getEmail());

        // 5. 回傳 200 OK 並附帶 JSON 資料
        return ResponseEntity.ok(response);
    }
}