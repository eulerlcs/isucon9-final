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
 * Table: train_timetable_master
 */
@Data
@NoArgsConstructor
// @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TrainTimetableMaster implements Serializable {
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
     * Column: station
     */
    private String station;

    /**
     * Column: departure
     */
    private LocalTime departure;

    /**
     * Column: arrival
     */
    private LocalTime arrival;
}