package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestSeat implements Serializable {

    private Integer row;

    private String column;
}