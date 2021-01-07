package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitializeResponse implements Serializable {
    @JsonProperty("available_days")
    private Integer availableDays;

    @JsonProperty("language")
    private String language;
}