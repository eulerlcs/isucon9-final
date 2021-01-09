package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Users;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

public interface UsersDao {
    public static final String TABLE_NAME = "train_master";

    @Delete("truncate users")
    void truncate();

    @Select("SELECT * FROM users WHERE id = #{id}")
    Users selectById(Long id);

    /**
     * @mbg.generated generated automatically, do not modify!
     */
    int insert(Users record);
}