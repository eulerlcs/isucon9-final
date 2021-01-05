package jp.zhimingsoft.www.isucon.dao;

public enum SeatClass {
    PREMIUM("premium"),
    RESERVED("reserved"),
    NON_RESERVED("non-reserved");

    private String value;

    SeatClass(String value) {
        this.value = value;
    }
}
