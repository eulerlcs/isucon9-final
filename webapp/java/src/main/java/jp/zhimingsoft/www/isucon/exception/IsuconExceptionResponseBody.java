package jp.zhimingsoft.www.isucon.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

import java.time.ZonedDateTime;

/**
 * copy from https://qiita.com/Keisuke1119/items/d13302582fbf933b5fa4
 */
@Setter
public class IsuconExceptionResponseBody {

    @JsonProperty("is_error")
    private boolean isError;
    @JsonProperty("message")
    private String message;

    @JsonIgnore
    private ZonedDateTime exceptionOccurrenceTime;

    @JsonIgnore
    private int status;

    @JsonIgnore
    private String errorMessage;

    @JsonIgnore
    private String path;
}