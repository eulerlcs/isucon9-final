package jp.zhimingsoft.www.isucon.dao;

import java.util.List;
import jp.zhimingsoft.www.isucon.domain.DistanceFareMaster;

public interface DistanceFareMasterMapper {

    int insert(DistanceFareMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insertSelective(DistanceFareMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    List<DistanceFareMaster> selectAll();
}