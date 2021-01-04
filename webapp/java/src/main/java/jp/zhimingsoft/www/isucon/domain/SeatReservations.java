package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: seat_reservations
 */
@Data
@NoArgsConstructor
public class SeatReservations implements Serializable {
    /**
     * Column: reservation_id
     */
    private Long reservationId;

    /**
     * Column: car_number
     */
    private Integer carNumber;

    /**
     * Column: seat_row
     */
    private Integer seatRow;

    /**
     * Column: seat_column
     */
    private String seatColumn;
}