package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.DistanceFareMaster;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public interface DistanceFareMasterDao {
    public static final String TABLE_NAME = "distance_fare_master";

    @SelectProvider(type = SqlProvider.class)
    List<DistanceFareMaster> selectOrderByDistance();


    class SqlProvider implements ProviderMethodResolver {
        public static String selectOrderByDistance() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                ORDER_BY("distance");
            }}.toString();
        }
    }
}