package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"date", "train_class", "train_name", "car_number", "seats", "cars"})
public class CarInformation implements Serializable {
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate date;

    private String trainClass;

    private String trainName;

    private Integer carNumber;

    @JsonProperty("seats")
    private List<SeatInformation> seatInformationList;

    private List<SimpleCarInformation> cars;
}