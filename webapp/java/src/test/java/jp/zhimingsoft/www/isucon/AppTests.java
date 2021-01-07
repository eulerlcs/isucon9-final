package jp.zhimingsoft.www.isucon;

import jp.zhimingsoft.www.isucon.domain.Settings;
import jp.zhimingsoft.www.isucon.domain.StationMaster;
import jp.zhimingsoft.www.isucon.service.MainService;
import jp.zhimingsoft.www.isucon.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AppTests {

    @Autowired
    private MainService mainService;

    @Test
    void contextLoads() {
        Settings settings = mainService.settingsHandler();

        Settings expected = new Settings();
        expected.setPaymentAPI(System.getenv().getOrDefault("PAYMENT_API", "http://localhost:5000"));
        Assertions.assertEquals(expected, settings);
    }

    @Test
    void getUsableTrainClassList() {
        StationMaster fromStation = new StationMaster(1L, "全部止まる", 10.0, true, true, true);
        List<String> ret = Utils.getUsableTrainClassList(fromStation, fromStation);
        Assertions.assertEquals(3, ret.size());

        fromStation = new StationMaster(1L, "ちょっと止まる", 10.0, false, true, true);
        ret = Utils.getUsableTrainClassList(fromStation, fromStation);
        Assertions.assertEquals(2, ret.size());

        fromStation = new StationMaster(1L, "各駅", 10.0, false, false, true);
        ret = Utils.getUsableTrainClassList(fromStation, fromStation);
        Assertions.assertEquals(1, ret.size());
    }
}
