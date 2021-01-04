package jp.zhimingsoft.www.isucon.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table: train_master
 */
@Data
@NoArgsConstructor
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
    private Boolean isNobori;
}