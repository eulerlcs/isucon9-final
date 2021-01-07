package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Table: reservations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservations implements Serializable {
    /**
     * Column: reservation_id
     */
    private Long reservationId;

    /**
     * Column: user_id
     */
    private Long userId;

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

    /**
     * Column: departure
     */
    private String departure;

    /**
     * Column: arrival
     */
    private String arrival;

    /**
     * Column: status
     */
    private String status;

    /**
     * Column: payment_id
     */
    private String paymentId;

    /**
     * Column: adult
     */
    private Integer adult;

    /**
     * Column: child
     */
    private Integer child;

    /**
     * Column: amount
     */
    private Long amount;
}