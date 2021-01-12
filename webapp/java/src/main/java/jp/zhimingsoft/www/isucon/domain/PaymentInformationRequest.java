package jp.zhimingsoft.www.isucon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInformationRequest implements Serializable {
    //    @JsonProperty("card_token")
    private String cardToken;

    //    @JsonProperty("reservation_id")
    private Integer reservationId;

    //    @JsonProperty("amount")
    private Integer amount;
}