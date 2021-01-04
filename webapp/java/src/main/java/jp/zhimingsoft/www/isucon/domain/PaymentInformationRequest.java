package jp.zhimingsoft.www.isucon.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class PaymentInformationRequest implements Serializable {

    private String cardToken;


    private Integer reservationId;


    private Integer amount;
}