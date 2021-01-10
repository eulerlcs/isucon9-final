package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: seat_reservations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservations implements Serializable {
    /**
     * Column: reservation_id
     */
    @JsonIgnore
    private Long reservationId;

    /**
     * Column: car_number
     */
    @JsonIgnore
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