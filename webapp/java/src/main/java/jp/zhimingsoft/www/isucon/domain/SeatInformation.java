package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
public class SeatInformation implements Serializable {

    private Integer row;


    private String column;


    private String seatClass;


    private Boolean isSmokingSeat;


    private Boolean isOccupied;
}