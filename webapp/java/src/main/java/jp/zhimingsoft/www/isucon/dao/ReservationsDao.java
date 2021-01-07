package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Reservations;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

public interface ReservationsDao {

    @Delete("truncate reservations")
    void truncate();

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int deleteByPrimaryKey(Long reservationId);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insert(Reservations record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insertSelective(Reservations record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    List<Reservations> selectAll();

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    Reservations selectByPrimaryKey(Long reservationId);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKeySelective(Reservations record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKey(Reservations record);
}