package com.example.investfeed.common.security

import mu.KotlinLogging
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * 부팅 시점에 모든 controller HandlerMethod 에 RequiresAction(permission, action) 이
 * 명시되어 있는지 검증. 하나라도 누락이면 IllegalStateException 으로 부팅 실패 (fail-fast).
 *
 * - SmartInitializingSingleton: 컨텍스트의 모든 싱글톤 초기화 직후 실행
 *   (ApplicationReadyEvent 보다 이른 시점, traffic 시작 전에 차단)
 * - PermissionInterceptor 가 제외하는 path 패턴 (api/auth) 은 검증 대상 외
 * - permission 또는 action 누락 시 모두 모아서 한 번에 출력
 */
@Component
class PermissionAnnotationValidator(
    private val handlerMapping: RequestMappingHandlerMapping,
    private val permissionResolver: PermissionResolver,
) : SmartInitializingSingleton {

    private val log = KotlinLogging.logger {}

    override fun afterSingletonsInstantiated() {
        val missing = mutableListOf<String>()

        handlerMapping.handlerMethods.forEach { (info, handler) ->
            val patterns = extractPatterns(info)

            if (patterns.isEmpty() || patterns.none { it.startsWith(SecurityPaths.API_PREFIX) }) return@forEach
            if (patterns.any { p -> SecurityPaths.PERMISSION_EXCLUDED_PREFIXES.any { p.startsWith(it) } }) return@forEach

            val resolved = permissionResolver.resolve(handler)
            if (resolved == null) {
                val controllerName = handler.beanType.simpleName
                val methodName = handler.method.name
                val pathStr = patterns.joinToString(",")
                missing += "  - $controllerName.$methodName  ($pathStr)"
            }
        }

        if (missing.isNotEmpty()) {
            val msg = buildString {
                appendLine("Permission annotation missing on ${missing.size} handler(s).")
                appendLine("모든 controller 메소드는 @RequiresAction(action = ...) 이 명시되어야 합니다 (HTTP method 컨벤션 fallback 제거됨).")
                appendLine("또는 클래스-level @RequiresAction 에 action 을 명시할 수 있습니다 (모든 메소드가 같은 action 일 때).")
                appendLine()
                missing.forEach { appendLine(it) }
            }
            log.error { msg }
            throw IllegalStateException(msg)
        }

        log.info { "PermissionAnnotationValidator: ${handlerMapping.handlerMethods.size} handler(s) validated, all annotated." }
    }

    private fun extractPatterns(info: RequestMappingInfo): Set<String> =
        info.pathPatternsCondition?.patternValues ?: emptySet()
}
