package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Reservations;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface ReservationsDao {
    public static final String TABLE_NAME = "reservations";

    @Delete("truncate reservations")
    void truncate();

    @Select("select * from reservations where reservation_id = #{id}")
    Reservations selectById(Long id);

    @Insert("INSERT INTO reservations (user_id, date, train_class, train_name, departure, arrival, status, payment_id, adult, child, amount) VALUES (#{user_id}, #{date}, #{train_class}, #{train_name}, #{departure}, #{arrival}, #{status}, #{payment_id}, #{adult}, #{child}, #{amount})")
    int insert(Reservations reservations);

    @Select("select * from reservations where reservation_id = #{id} FOR UPDATE")
    Reservations selectByIdForUpdate(Long id);

    @Select("select * from reservations where date = #{date} and train_class = #{train_class} and train_name = #{train_name} FOR UPDATE")
    List<Reservations> selectByDateClassNameForUpdate(LocalDate date, String train_class, String train_name);
}