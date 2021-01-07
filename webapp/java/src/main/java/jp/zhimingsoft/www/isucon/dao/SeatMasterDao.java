package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.SeatMaster;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public interface SeatMasterDao {
    public static final String TABLE_NAME = "seat_master";

    @SelectProvider(sqlProvider.class)
    List<SeatMaster> selectSeatList(String train_class, String seat_class, boolean is_smoking_seat);

    @SelectProvider(sqlProvider.class)
    List<SeatMaster> selectByClassNumber(String train_class, int car_number);

    @SelectProvider(sqlProvider.class)
    SeatMaster selectOneByClassNumber(String train_class, int car_number);

    class sqlProvider implements ProviderMethodResolver {
        public String selectSeatList() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("train_class = #{train_class}");
                WHERE("seat_class = #{seat_class}");
                WHERE("is_smoking_seat = #{is_smoking_seat}");
            }}.toString();
        }

        public String selectByClassNumber() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("train_class = #{train_class}");
                WHERE("car_number = #{car_number}");
                ORDER_BY("seat_row, seat_column");
            }}.toString();
        }

        public String selectOneByClassNumber() {
            return new SQL() {{
                SELECT("*");
                FROM(TABLE_NAME);
                WHERE("train_class = #{train_class}");
                WHERE("car_number = #{car_number}");
                ORDER_BY("seat_row, seat_column");
                LIMIT(1);
            }}.toString();
        }
    }
}