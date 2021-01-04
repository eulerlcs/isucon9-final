package jp.zhimingsoft.www.isucon.utils;

import java.util.List;

public class DbUtils {
    /**
     * リストを受け取って IN () 句で利用するパラメータ文字列を返す
     * 例: #{shopCodes[0]}, #{shopCodes[1]}, #{shopCodes[2]}
     */
    public static <T> String getInPhraseParamString(String paramName, List<T> valueList) {
        if (valueList == null || valueList.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valueList.size(); i++) {
            sb.append(", " + String.format("#{%s[%s]}", paramName, i));
        }

        return sb.toString().substring(2);
    }
}
