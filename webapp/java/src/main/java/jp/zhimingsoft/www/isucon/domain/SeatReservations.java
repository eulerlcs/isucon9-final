package jp.zhimingsoft.www.isucon.domain;

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