package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class ReservationResponse implements Serializable {
    /**
     * Column: reservation_id
     */
    private Long reservationId;

    /**
     * Column: date
     */
    private Date date;

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