package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class TrainReservationResponse implements Serializable {

    private Long reservationId;


    private Integer amount;


    private Boolean isOk;
}