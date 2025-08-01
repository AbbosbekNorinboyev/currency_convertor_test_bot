package uz.brb.test_bot.entity;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CurrencyRate {
    private String id;
    private String Code;
    private String Ccy;
    private String CcyNm_RU;
    private String CcyNm_UZ;
    private String CcyNm_UZC;
    private String CcyNm_EN;
    private String Nominal;
    private String Rate;
    private String Diff;
    private String Date;
}