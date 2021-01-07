package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Table: station_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationMaster implements Serializable {
    /**
     * Column: id
     */
    private Long id;

    /**
     * Column: name
     */
    private String name;

    /**
     * Column: distance
     */
    @JsonIgnore
    private Double distance;


    /**
     * Column: is_stop_express
     */
    private Boolean isStopExpress;

    /**
     * Column: is_stop_semi_express
     */
    private Boolean isStopSemiExpress;

    /**
     * Column: is_stop_local
     */
    private Boolean isStopLocal;
}