package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.StationMaster;
import jp.zhimingsoft.www.isucon.utils.ZeroOneBooleanTypeHandler;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public interface StationMasterDao {
    public static final String TABLE_NAME = "station_master";

    @SelectProvider(type = SqlProvider.class)
//    @Results(id = "self",
//            value = {
//                    @Result(property = "id", column = "id", id = true),
//                    @Result(property = "name", column = "name"),
//                    @Result(property = "distance", column = "distance"),
//                    @Result(property = "isStopExpress", column = "is_stop_express", typeHandler = ZeroOneBooleanTypeHandler.class),
//                    @Result(property = "isStopSemiExpress", column = "is_stop_semi_express", typeHandler = ZeroOneBooleanTypeHandler.class),
//                    @Result(property = "isStopLocal", column = "is_stop_local", typeHandler = ZeroOneBooleanTypeHandler.class),
//            })
    StationMaster selectByName(String name);

    @SelectProvider(type = SqlProvider.class)
//    @ResultMap("self")
    List<StationMaster> selectAllByDistanceDesc();

    @SelectProvider(type = SqlProvider.class)
//    @ResultMap("self")
    List<StationMaster> selectAllByDistanceAsc();

    class SqlProvider implements ProviderMethodResolver {
        public static String selectByName(String name) {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("name = #{name} ");
            }}.toString();
        }

        public static String selectAllByDistanceDesc() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                ORDER_BY("distance DESC");
            }}.toString();
        }

        public static String selectAllByDistanceAsc() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                ORDER_BY("distance ASC");
            }}.toString();
        }
    }
}