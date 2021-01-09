package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainReservationRequest implements Serializable {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime date;

    private String trainName;

    private String trainClass;

    private Integer carNumber;

    private boolean isSmokingSeat;

    private String seatClass;

    private String departure;

    private String arrival;

    private Integer child;

    private Integer adult;

    private String column;

    private List<RequestSeat> seats;

}