package com.example.demo.mapper;

import com.example.demo.entity.Stores;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

@Mapper
public interface StoresMapper extends BaseMapper<Stores> {
}
