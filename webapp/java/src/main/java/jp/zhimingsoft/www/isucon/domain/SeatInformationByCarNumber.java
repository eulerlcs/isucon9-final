package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class SeatInformationByCarNumber implements Serializable {

    private Integer carNumber;


    private List<SeatInformation> seatInformationList;
}