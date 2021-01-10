package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Reservations;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

public interface ReservationsDao {
    public static final String TABLE_NAME = "reservations";

    @Delete("truncate reservations")
    void truncate();

    @Select("select * from reservations where reservation_id = #{reservation_id}")
    Reservations selectByReservationId(Long reservation_id);

    @Insert("INSERT INTO reservations (user_id, date, train_class, train_name, departure, arrival, status, payment_id, adult, child, amount) VALUES (#{userId}, #{date}, #{trainClass}, #{trainName}, #{departure}, #{arrival}, #{status}, #{paymentId}, #{adult}, #{child}, #{amount})")
    @Options(useGeneratedKeys = true, keyProperty = "reservationId")
    int insert(Reservations reservations);

    @Select("select * from reservations where reservation_id = #{id} FOR UPDATE")
    Reservations selectByIdForUpdate(Long id);

    @Select("select * from reservations where date = #{date} and train_class = #{train_class} and train_name = #{train_name} FOR UPDATE")
    List<Reservations> selectByDateClassNameForUpdate(LocalDate date, String train_class, String train_name);

    @Select("SELECT * FROM reservations WHERE user_id = #{user_id}")
    List<Reservations> selectByUserId(Long user_id);

    @Select("SELECT * FROM reservations WHERE reservation_id = #{reservation_id} and user_id = #{user_id}")
    Reservations selectByReservationIdUserId(Long reservation_id, Long user_id);

    @Delete("DELETE FROM reservations WHERE reservation_id = #{reservation_id} and user_id = #{user_id}")
    int delete(Long reservation_id, Long user_id);

    @Update("UPDATE reservations SET status = #{status}, payment_id = #{paymentId} WHERE reservation_id = #{reservationId}")
    int update(Reservations record);
}