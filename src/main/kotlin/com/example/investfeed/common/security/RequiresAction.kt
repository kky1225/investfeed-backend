package com.example.investfeed.common.security

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresAction(
    val permission: String = "",
    val action: String = "",
)