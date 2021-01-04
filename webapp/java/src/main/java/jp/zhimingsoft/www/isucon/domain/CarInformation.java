package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class CarInformation implements Serializable {

    private Date date;


    private String trainClass;


    private String trainName;


    private Integer carNumber;


    private List<SeatInformation> seatInformationList;


    private List<SimpleCarInformation> cars;
}