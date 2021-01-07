package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarInformation implements Serializable {

    private LocalDate date;


    private String trainClass;


    private String trainName;


    private Integer carNumber;


    private List<SeatInformation> seatInformationList;


    private List<SimpleCarInformation> cars;
}