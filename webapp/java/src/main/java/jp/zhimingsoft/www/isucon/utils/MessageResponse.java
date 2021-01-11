package jp.zhimingsoft.www.isucon.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"is_error", "message"})
public class MessageResponse {
    @JsonProperty("is_error")
    private boolean error;
    private String message;

    public MessageResponse(String message) {
        this.error = false;
        this.message = message;
    }
}