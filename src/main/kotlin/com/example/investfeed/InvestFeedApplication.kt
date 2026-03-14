package com.example.investfeed

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class InvestFeedApplication

fun main(args: Array<String>) {
    runApplication<InvestFeedApplication>(*args)
}
