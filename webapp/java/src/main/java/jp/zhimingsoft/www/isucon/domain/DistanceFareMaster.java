package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: distance_fare_master
 */
@Data
@NoArgsConstructor
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