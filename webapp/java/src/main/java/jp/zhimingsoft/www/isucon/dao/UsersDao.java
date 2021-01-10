package jp.zhimingsoft.www.isucon.dao;

import jp.zhimingsoft.www.isucon.domain.Users;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UsersDao {
    public static final String TABLE_NAME = "train_master";

    @Delete("truncate users")
    void truncate();

    @Select("SELECT * FROM users WHERE id = #{id}")
    Users selectById(Long id);

    @Select("SELECT * FROM users WHERE email = #{email}")
    Users selectByEmail(String email);

    @Insert("INSERT INTO users (email, salt, super_secure_password) VALUES (#{email}, #{salt}, #{superSecurePassword})")
    @Options(keyProperty = "id")
    int insert(Users record);
}