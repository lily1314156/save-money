package com.example.demo.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis TypeHandler：在「資料庫 JSON 字串」與「Java List&lt;String&gt;」之間轉換。
 *
 * 用法：在 mapper XML 的 <result> 上指定
 *      typeHandler="com.example.demo.typehandler.JsonStringListTypeHandler"
 *
 * 寫入 DB：List → JSON 字串（["xxx","yyy"]）
 * 讀出 DB：JSON 字串 → List
 */
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    // ObjectMapper 是執行緒安全的，做成 static 全應用共用一個就好（省記憶體）
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // TypeReference 告訴 Jackson 要把 JSON 反序列化成 List<String>，不是 List<Object>
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    /** 寫入：把 List<String> 轉 JSON 字串，塞進 SQL ? 的位置 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("List<String> 序列化成 JSON 失敗", e);
        }
    }

    /** 讀出：依 columnName 抓欄位字串，解析回 List<String> */
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String json) throws SQLException {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (Exception e) {
            throw new SQLException("JSON 反序列化失敗：" + json, e);
        }
    }
}
