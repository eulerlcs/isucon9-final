package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.TrainMaster;
import jp.zhimingsoft.www.isucon.utils.DbUtils;
import jp.zhimingsoft.www.isucon.utils.ZeroOneBooleanTypeHandler;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;
import java.util.List;

public interface TrainMasterDao {
    @Results(id = "self",
            value = {
                    @Result(property = "date", column = "date"),
                    @Result(property = "departureAt", column = "departure_at"),
                    @Result(property = "trainClass", column = "train_class"),
                    @Result(property = "trainName", column = "train_name"),
                    @Result(property = "startStation", column = "start_station"),
                    @Result(property = "lastStation", column = "last_station"),
                    @Result(property = "isNobori", column = "is_nobori", typeHandler = ZeroOneBooleanTypeHandler.class),
            })
    @SelectProvider(type = SqlProvider.class)
    List<TrainMaster> selectByDateClassNobori(LocalDate date, @Param("train_class") List<String> trainClassList, @Param("is_nobori") boolean isNobori);

    class SqlProvider implements ProviderMethodResolver {
        public static final String TABLE_NAME = "train_master";

        public static String selectByDateClassNobori(@Param("train_class") List<String> trainClassList) {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("date = #{date} ");
                WHERE("train_class in (" + DbUtils.getInPhraseParamString("train_class", trainClassList) + ")");
                WHERE("is_nobori = #{is_nobori} ");
            }}.toString();
        }
    }
}