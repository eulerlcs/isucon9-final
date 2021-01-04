package jp.zhimingsoft.www.isucon;

import jp.zhimingsoft.www.isucon.domain.Settings;
import jp.zhimingsoft.www.isucon.service.MainService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppTests {

    @Autowired
    private MainService mainService;

    @Test
    void contextLoads() {
        Settings settings = mainService.settingsHandler();

        Settings expected = new Settings();
        expected.setPaymentAPI(System.getenv().getOrDefault("PAYMENT_API", "http://localhost:5000"));
        Assertions.assertEquals(expected,settings);
    }

}
