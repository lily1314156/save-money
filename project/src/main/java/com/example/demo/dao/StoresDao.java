package com.example.demo.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Stores;
import com.example.demo.mapper.StoresMapper;
import java.util.List;

@Repository
public class StoresDao {

    @Autowired
    private StoresMapper storesMapper;

    public List<Stores> selectAllStores() {
        LambdaQueryWrapper<Stores> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stores::getIsActive, true);
        return storesMapper.selectList(wrapper);
    }

    //取得指定品牌的所有啟用分店
    public List<Stores> selectBrandId(Integer brandId) {
        LambdaQueryWrapper<Stores> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stores::getBrandId, brandId)
               .eq(Stores::getIsActive, true);
        return storesMapper.selectList(wrapper);
    }

    /** 新增一筆店家（B7：Places API 撈到的新店要 INSERT 進 DB） */
    public int insert(Stores store) {
        return storesMapper.insert(store);
    }

    /**
     * 判斷某品牌的店在指定座標是否已經存在。
     * 避免 Places API 回的同一家店重複 INSERT
     */
    public boolean existsByBrandAndCoords(Integer brandId, Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        LambdaQueryWrapper<Stores> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stores::getBrandId, brandId)
               .eq(Stores::getLat, lat)
               .eq(Stores::getLng, lng);
        return storesMapper.exists(wrapper);
    }
}