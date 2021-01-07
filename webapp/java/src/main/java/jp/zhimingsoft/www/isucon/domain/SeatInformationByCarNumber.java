package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatInformationByCarNumber implements Serializable {

    private Integer carNumber;


    private List<SeatInformation> seatInformationList;
}