package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Table: fare_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareMaster implements Serializable {
    /**
     * Column: train_class
     */
    @JsonProperty("train_class")
    private String trainClass;

    /**
     * Column: seat_class
     */
    @JsonProperty("seat_class")
    private String seatClass;

    /**
     * Column: start_date
     */
    @JsonProperty("start_date")
    private LocalDate startDate;

    /**
     * Column: fare_multiplier
     */
    @JsonProperty("fare_multiplier")
    private Double fareMultiplier;
}