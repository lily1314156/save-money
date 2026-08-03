package com.example.demo.mapper;

import com.example.demo.entity.Brands;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface BrandsMapper extends BaseMapper<Brands> {
}