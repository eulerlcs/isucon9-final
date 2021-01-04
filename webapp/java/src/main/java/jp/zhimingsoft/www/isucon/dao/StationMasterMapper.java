package jp.zhimingsoft.www.isucon.dao;

import java.util.List;
import jp.zhimingsoft.www.isucon.domain.StationMaster;
import org.apache.ibatis.annotations.Param;

public interface StationMasterMapper {
    /**
     * @mbg.generated generated automatically, do not modify!
     */
    long countByExample(StationMasterDao example);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int deleteByExample(StationMasterDao example);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insert(StationMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insertSelective(StationMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    List<StationMaster> selectByExample(StationMasterDao example);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    StationMaster selectByPrimaryKey(Long id);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByExampleSelective(@Param("record") StationMaster record, @Param("example") StationMasterDao example);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByExample(@Param("record") StationMaster record, @Param("example") StationMasterDao example);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKeySelective(StationMaster record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKey(StationMaster record);
}