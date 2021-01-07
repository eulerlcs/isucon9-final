package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"row", "column", "class", "is_smoking_seat", "is_occupied"})
public class SeatInformation implements Serializable {
    private Integer row;

    private String column;

    @JsonProperty(value = "class")
    private String seatClass;

    private Boolean isSmokingSeat;

    private Boolean isOccupied;
}