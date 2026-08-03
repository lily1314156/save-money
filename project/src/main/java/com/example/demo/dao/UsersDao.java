package com.example.demo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Users;
import com.example.demo.mapper.UsersMapper;

@Repository
public class UsersDao {

    @Autowired
    private UsersMapper usersMapper;

    /** 用 email 找使用者（登入、忘記密碼都靠這個） */
    public Users selectByEmail(String email) {
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getEmail, email);
        return usersMapper.selectOne(wrapper);
    }

    /** 用 id 找使用者（會員頁顯示資料用，確保拿到的是 DB 最新狀態） */
    public Users selectById(Integer id) {
        return usersMapper.selectById(id);
    }

    /** 註冊前檢查 email 有沒有被用過 */
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getEmail, email);
        return usersMapper.exists(wrapper);
    }

    public void insert(Users user) {
        usersMapper.insert(user);
    }

    /** 只更新密碼欄位（updateById 遇到 null 欄位會跳過不動） */
    public void updatePassword(Integer id, String hashedPassword) {
        Users user = Users.builder()
            .id(id)
            .password(hashedPassword)
            .build();
        usersMapper.updateById(user);
    }
}
