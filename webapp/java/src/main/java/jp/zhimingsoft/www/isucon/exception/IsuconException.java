package jp.zhimingsoft.www.isucon.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * copy from https://qiita.com/Keisuke1119/items/d13302582fbf933b5fa4
 */
@Getter
public class IsuconException extends RuntimeException {
    private Boolean isError;
    private HttpStatus httpStatus;

    public IsuconException(String message, HttpStatus httpStatus) {
        super(message);
        this.isError = true;
        this.httpStatus = httpStatus;
    }

    public IsuconException(String message) {
        super(message);
        this.isError = false;
        this.httpStatus = HttpStatus.OK;
    }
}