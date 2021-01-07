package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: distance_fare_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceFareMaster implements Serializable {
    /**
     * Column: distance
     */
    @JsonProperty("distance")
    private Double distance;

    /**
     * Column: fare
     */
    @JsonProperty("fare")
    private Integer fare;
}