package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Reservations;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

public interface ReservationsDao {

    @Delete("truncate reservations")
    void truncate();

    @Select("select * from reservations where reservation_id = #{id}")
    Reservations selectById(Long id);

}