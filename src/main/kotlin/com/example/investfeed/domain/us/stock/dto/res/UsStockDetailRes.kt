package com.example.investfeed.domain.us.stock.dto.res

data class UsStockDetailRes(
    val usStockInfo: UsStockInfo,
    val chartList: List<UsStockChart>,
    val dailyPriceList: List<UsStockDailyPrice>,
)

data class UsStockInfo(
    val stexTp: String?, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    val stkCd: String?, // 종목코드 (티커)
    val stkNm: String?, // 종목명
    val stkEnm: String?, // 종목영문명
    val curPrc: String?, // 현재가 (USD)
    val predPreSig: String?, // 전일대비기호
    val predPre: String?, // 전일대비
    val fluRt: String?, // 등락률
    val accTrdeQty: String?, // 누적거래량
    val baseExrt: String?, // 환율
    val wk52HgstPric: String?, // 52주 최고가
    val wk52HgstPricDt: String?, // 52주 최고가 일자
    val wk52HgstPricPreRt: String?, // 52주 최고가 대비율
    val wk52LwstPric: String?, // 52주 최저가
    val wk52LwstPricDt: String?, // 52주 최저가 일자
    val wk52LwstPricPreRt: String?, // 52주 최저가 대비율
    val preOpenPric: String?, // 전일 시가
    val preHighPric: String?, // 전일 고가
    val preLowPric: String?, // 전일 저가
    val baseClosePric: String?, // 기준종가 (전일종가)
    val openPric: String?, // 당일 시가
    val highPric: String?, // 당일 고가
    val lowPric: String?, // 당일 저가
    val stkCnt: String?, // 상장주식수
    val mac: String?, // 시가총액 (천 USD)
    val lgIndsCd: String?, // 업종 대분류
    val smIndsCd: String?, // 업종 소분류
    val currUnit: String?, // 통화단위
    val trdSuspTp: String?, // 거래정지구분 0:정상
)

data class UsStockChart(
    val dt: String?, // 일자 (분봉은 체결시각 yyyyMMddHHmmss)
    val curPrc: String?, // 종가
    val openPric: String?, // 시가
    val highPric: String?, // 고가
    val lowPric: String?, // 저가
    val trdeQty: String?, // 거래량
    val trdePrica: String?, // 거래대금
)

data class UsStockDailyPrice(
    val dt: String?, // 일자
    val curPrc: String?, // 종가
    val predPreSig: String?, // 전일대비기호
    val predPre: String?, // 전일대비
    val fluRt: String?, // 등락률
    val openPric: String?, // 시가
    val highPric: String?, // 고가
    val lowPric: String?, // 저가
    val accTrdeQty: String?, // 누적거래량
    val trdePrica: String?, // 거래대금
)
