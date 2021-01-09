package jp.zhimingsoft.www.isucon.domain;

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

    private boolean isOk;
}