package com.example.demo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Brands;
import com.example.demo.mapper.BrandsMapper;

import java.util.List;

@Repository
public class BrandsDao {

    @Autowired
    private BrandsMapper brandsMapper;

    /** 取得所有啟用中的品牌，依 sort_order 排序 */
    public List<Brands> selectAllBrands() {
        LambdaQueryWrapper<Brands> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Brands::getIsActive, true)   // .eq(Entity 裡的 isActive 屬性, 1)
               .orderByAsc(Brands::getSortOrder);   //依 sort_order 排序品牌。
        return brandsMapper.selectList(wrapper);
    }

    /** 用 slug 找品牌（例：'fifty'、'starbucks'） */
    public Brands selectBySlug(String slug) {
        LambdaQueryWrapper<Brands> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Brands::getSlug, slug);
        return brandsMapper.selectOne(wrapper);
    }

    /** 用 id 找品牌 */
    public Brands selectById(Integer id) {
        return brandsMapper.selectById(id);
    }

    /** 檢查 slug 是否存在 */
    public boolean existsBySlug(String slug) {
        LambdaQueryWrapper<Brands> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Brands::getSlug, slug);
        return brandsMapper.exists(wrapper);
    }
}