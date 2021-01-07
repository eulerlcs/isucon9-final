package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Table: train_timetable_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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