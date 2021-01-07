package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse implements Serializable {
    /**
     * Column: reservation_id
     */
    private Long reservationId;

    /**
     * Column: date
     */
    private LocalDate date;

    /**
     * Column: train_class
     */
    private String trainClass;

    /**
     * Column: train_name
     */
    private String trainName;


    private Integer carNumber;


    private String seatClass;

    /**
     * Column: amount
     */
    private Integer amount;

    /**
     * Column: adult
     */
    private Integer adult;

    /**
     * Column: child
     */
    private Integer child;

    /**
     * Column: departure
     */
    private String departure;

    /**
     * Column: arrival
     */
    private String arrival;


    private String departureTime;


    private String arrivalTime;


    private List<SeatReservations> seats;
}