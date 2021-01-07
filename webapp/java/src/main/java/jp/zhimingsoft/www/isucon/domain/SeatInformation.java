package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatInformation implements Serializable {

    private Integer row;


    private String column;


    private String seatClass;


    private Boolean isSmokingSeat;


    private Boolean isOccupied;
}