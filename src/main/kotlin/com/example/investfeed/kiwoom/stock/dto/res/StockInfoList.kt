package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfoList (
    var code: String? = null, // 종목코드
    var name: String? = null, // 종목명
    var listCount: String? = null, // 상장주식수
    var auditInfo: String? = null, // 감리구분
    var regDay: String? = null, // 상장일
    var lastPrice: String? = null, // 전일종가
    var state: String? = null, // 종목상태
    var marketCode: String? = null, // 시장구분코드
    var marketName: String? = null, // 시장명
    var upName: String? = null, // 업종명
    var upSizeName: String? = null, // 회사크기분류
    var companyClassName: String? = null, // 회사분류
    var orderWarning: String? = null, // 투자유의종목여부 0: 해당없음, 2: 정리매매, 3: 단기과열, 4: 투자위험, 5: 투자경과, 1: ETF투자주의요망(ETF인 경우만 전달)
    var nxtEnable: String? = null // 가능여부
)