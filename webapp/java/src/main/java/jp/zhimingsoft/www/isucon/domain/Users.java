package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: users
 */
@Data
@NoArgsConstructor
// @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Users implements Serializable {
    /**
     * Column: id
     */
    private Long id;

    /**
     * Column: email
     */
    private String email;

    /**
     * Column: salt
     */
    private byte[] salt;

    /**
     * Column: super_secure_password
     */
    private byte[] superSecurePassword;
}