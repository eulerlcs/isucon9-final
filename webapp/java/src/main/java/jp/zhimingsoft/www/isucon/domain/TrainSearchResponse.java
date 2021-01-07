package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"trainClass", "trainName", "start", "last", "departure", "arrival", "departureTime", "arrivalTime", "seatAvailability", "seat_fare"})
public class TrainSearchResponse implements Serializable {
    private String trainClass;

    private String trainName;

    private String start;

    private String last;

    private String departure;

    private String arrival;

    private LocalTime departureTime;

    private LocalTime arrivalTime;

    @JsonPropertyOrder(alphabetic = true)
    private Map<String, String> seatAvailability;

    @JsonProperty("seat_fare")
    @JsonPropertyOrder(alphabetic = true)
    private Map<String, Integer> fare;
}