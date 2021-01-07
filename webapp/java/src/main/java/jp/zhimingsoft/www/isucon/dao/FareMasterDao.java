package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.FareMaster;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public interface FareMasterDao {
    public static final String TABLE_NAME = "fare_master";

    @SelectProvider(type = SqlProvider.class)
    List<FareMaster> selectByTrainSeat(String train_class, String seat_class);


    class SqlProvider implements ProviderMethodResolver {
        public static String selectByTrainSeat() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("train_class = #{train_class}");
                WHERE("seat_class = #{seat_class}");
                ORDER_BY("start_date");
            }}.toString();
        }
    }
}