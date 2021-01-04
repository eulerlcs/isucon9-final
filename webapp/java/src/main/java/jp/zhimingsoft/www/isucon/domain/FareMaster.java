package jp.zhimingsoft.www.isucon.domain;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table: fare_master
 */
@Data
@NoArgsConstructor
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
    private Date startDate;

    /**
     * Column: fare_multiplier
     */
    @JsonProperty("fare_multiplier")
    private Double fareMultiplier;
}