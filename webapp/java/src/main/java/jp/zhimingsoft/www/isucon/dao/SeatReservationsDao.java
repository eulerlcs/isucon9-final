package jp.zhimingsoft.www.isucon.dao;

import java.util.List;
import jp.zhimingsoft.www.isucon.domain.SeatReservations;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

public interface SeatReservationsDao {

    @Delete("truncate seat_reservations")
    void truncate();

    int insert(SeatReservations record);

    List<SeatReservations> selectAll();
}