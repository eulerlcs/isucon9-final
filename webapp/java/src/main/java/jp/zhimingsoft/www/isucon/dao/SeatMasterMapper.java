package jp.zhimingsoft.www.isucon.dao;

import java.util.List;
import jp.zhimingsoft.www.isucon.domain.SeatMaster;

public interface SeatMasterMapper {
    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insert(SeatMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insertSelective(SeatMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    List<SeatMaster> selectAll();
}