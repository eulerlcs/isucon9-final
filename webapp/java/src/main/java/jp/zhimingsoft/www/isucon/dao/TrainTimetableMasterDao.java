package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.TrainTimetableMaster;
import jp.zhimingsoft.www.isucon.utils.DbUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;
import java.util.List;

public interface TrainTimetableMasterDao {

    @SelectProvider(type = SqlProvider.class)
    TrainTimetableMaster selectOne(LocalDate date,  String trainClass, String trainName, String station);

    class SqlProvider implements ProviderMethodResolver {
        public static final String TABLE_NAME = "train_timetable_master";

        public static String selectOne() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("date = #{date} ");
                WHERE("train_class = #{trainClass}");
                WHERE("train_name = #{trainName} ");
                WHERE("station = #{station} ");
            }}.toString();
        }
    }
}