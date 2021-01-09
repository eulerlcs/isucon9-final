package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.SeatReservations;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;
import java.util.List;

public interface SeatReservationsDao {
    public static final String TABLE_NAME = "seat_reservations";

    @Delete("truncate seat_reservations")
    void truncate();

    @SelectProvider(sqlProvider.class)
    List<SeatReservations> selectReservedSeatList(boolean is_nobori, Long from_station_id, Long to_station_id);

    @SelectProvider(sqlProvider.class)
    List<SeatReservations> selectSeatReservationList(LocalDate date, String train_class, String train_name, Integer car_number, Integer seat_row, String seat_column);

    @SelectProvider(sqlProvider.class)
    List<SeatReservations> selectSeatReservationListForUpdate(LocalDate date, String train_class, String train_name, Integer car_number, Integer seat_row, String seat_column);

    @Select("SELECT * FROM seat_reservations WHERE reservation_id = #{reservation_id} FOR UPDATE")
    List<SeatReservations> selectByIdForUpdate(long reservation_id);

    @Insert("INSERT INTO seat_reservations (reservation_id, car_number, seat_row, seat_column) VALUES (#{reservation_id}, #{car_number}, #{seat_row}, #{seat_column})")
    int insert(SeatReservations seatReservations);


    class sqlProvider implements ProviderMethodResolver {
        public String selectReservedSeatList(boolean is_nobori, Long from_station_id, Long to_station_id) {
            return new SQL() {{
                SELECT("sr.*");
                FROM("seat_reservations sr, reservations r, seat_master s, station_master std, station_master sta");
                WHERE("r.reservation_id=sr.reservation_id");
                WHERE("s.train_class=r.train_class");
                WHERE("s.car_number=sr.car_number");
                WHERE("s.seat_column=sr.seat_column");
                WHERE("s.seat_row=sr.seat_row");
                WHERE("std.name=r.departure");
                WHERE("sta.name=r.arrival");

                if (is_nobori) {
                    WHERE(" ((sta.id <  #{from_station_id} AND #{from_station_id} <= std.id) OR (sta.id <  #{to_station_id} AND #{to_station_id} <= std.id) OR (#{from_station_id} < sta.id AND std.id < #{to_station_id}))");
                } else {
                    WHERE(" ((std.id <= #{from_station_id} AND #{from_station_id}  < sta.id) OR (std.id <= #{to_station_id} AND #{to_station_id}  < sta.id) OR (sta.id < #{from_station_id} AND #{to_station_id} < std.id))");
                }
            }}.toString();
        }

        public String selectSeatReservationList() {
            return new SQL() {{
                SELECT("s.*");
                FROM("seat_reservations s, reservations r");
                WHERE("r.date = #{date}");
                WHERE("r.train_class = #{train_class}");
                WHERE("r.train_name = #{train_name}");
                WHERE("car_number = #{car_number}");
                WHERE("seat_row = #{seat_row}");
                WHERE("seat_column = #{seat_column}");
            }}.toString();
        }

        public String selectSeatReservationListForUpdate() {
            return new SQL() {{
                SELECT("s.*");
                FROM("seat_reservations s, reservations r");
                WHERE("r.date = #{date}");
                WHERE("r.train_class = #{train_class}");
                WHERE("r.train_name = #{train_name}");
                WHERE("car_number = #{car_number}");
                WHERE("seat_row = #{seat_row}");
                WHERE("seat_column = #{seat_column}");
            }}.toString() + " FOR UPDATE";
        }
    }
}