package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainReservationResponse implements Serializable {
    private Long reservationId;

    private Integer amount;

    @JsonProperty("is_ok")
    private boolean isOk;
}