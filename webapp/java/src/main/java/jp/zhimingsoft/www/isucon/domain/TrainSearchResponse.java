package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainSearchResponse implements Serializable {
    /**
     * Column: train_class
     */
    private String trainClass;

    /**
     * Column: train_name
     */
    private String trainName;


    private String start;


    private String last;

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


    private Map<String, String> seatAvailability;


    private Map<String, Integer> fare;
}