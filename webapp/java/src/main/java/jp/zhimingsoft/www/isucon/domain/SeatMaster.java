package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: seat_master
 */
@Data
@NoArgsConstructor
public class SeatMaster implements Serializable {
    /**
     * Column: train_class
     */
    private String trainClass;

    /**
     * Column: car_number
     */
    private Integer carNumber;

    /**
     * Column: seat_column
     */
    private String seatColumn;

    /**
     * Column: seat_row
     */
    private Integer seatRow;

    /**
     * Column: seat_class
     */
    private String seatClass;

    /**
     * Column: is_smoking_seat
     */
    private Boolean isSmokingSeat;
}