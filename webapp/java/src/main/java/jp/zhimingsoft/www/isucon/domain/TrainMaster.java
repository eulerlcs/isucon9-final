package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Table: train_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
// @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TrainMaster implements Serializable {
    /**
     * Column: date
     */
    private LocalDate date;

    /**
     * Column: departure_at
     */
    private LocalTime departureAt;

    /**
     * Column: train_class
     */
    private String trainClass;

    /**
     * Column: train_name
     */
    private String trainName;

    /**
     * Column: start_station
     */
    private String startStation;

    /**
     * Column: last_station
     */
    private String lastStation;

    /**
     * Column: is_nobori
     */
    @JsonProperty("is_nobori")
    private boolean isNobori;
}