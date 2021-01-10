package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users implements Serializable {
    /**
     * Column: id
     */
    private Long id;

    /**
     * Column: email
     */
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * Column: salt
     */
    private byte[] salt;

    /**
     * Column: super_secure_password
     */
    private byte[] superSecurePassword;
}