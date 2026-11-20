package com.example.investfeed.domain.auth.dto.res

data class SecondaryPasswordLockStatusRes(
    val suspended: Boolean,             // 여러 번 틀려 2차 비밀번호가 정지됐는지. 관리자가 초기화해야 풀린다
    val remainingAttempts: Int,         // 이만큼 더 틀리면 정지된다
    // 2차 인증이 유지되는 남은 초. 인증하지 않았거나 풀렸으면 0. 화면이 상단의 자물쇠와 남은 시간을 그릴 때 쓴다
    val authRemainingSeconds: Long = 0,
    // 2차 비밀번호를 만들어 두었는지. 화면이 자물쇠를 눌렀을 때 인증 창과 설정 창 중 무엇을 띄울지 정한다
    // (로그인 때 받은 값은 다른 기기에서 만든 뒤에도 그대로라 쓰지 않는다)
    val passwordSet: Boolean = true,
)
