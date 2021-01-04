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
    private Boolean isError;
    @JsonProperty("message")
    private String message;

    @JsonIgnore
    // @JsonProperty("timestamp")
    private ZonedDateTime exceptionOccurrenceTime;

    @JsonIgnore
    // @JsonProperty("status")
    private int status;

    @JsonIgnore
    // @JsonProperty("error")
    private String error;

    @JsonIgnore
    // @JsonProperty("path")
    private String path;
}