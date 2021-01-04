package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Users;
import org.apache.ibatis.annotations.Delete;

public interface UsersDao {

    @Delete("truncate users")
    void truncate();

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int deleteByPrimaryKey(Long id);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insert(Users record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insertSelective(Users record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    Users selectByPrimaryKey(Long id);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKeySelective(Users record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKeyWithBLOBs(Users record);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int updateByPrimaryKey(Users record);
}