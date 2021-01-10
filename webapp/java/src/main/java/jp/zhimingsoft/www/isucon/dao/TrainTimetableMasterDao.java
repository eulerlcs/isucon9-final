package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.TrainTimetableMaster;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;

public interface TrainTimetableMasterDao {
    public static final String TABLE_NAME = "train_timetable_master";

    @SelectProvider(type = SqlProvider.class)
    TrainTimetableMaster selectOne(LocalDate date, String train_class, String train_name, String station);


    class SqlProvider implements ProviderMethodResolver {
        public static String selectOne() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("date = #{date} ");
                WHERE("train_class = #{train_class}");
                WHERE("train_name = #{train_name} ");
                WHERE("station = #{station} ");
            }}.toString();
        }
    }
}