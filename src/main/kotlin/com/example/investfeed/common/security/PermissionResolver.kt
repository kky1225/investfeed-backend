package com.example.investfeed.common.security

import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

@Component
class PermissionResolver {

    fun resolve(handler: HandlerMethod): Pair<String, String>? {
        val methodAnno = handler.getMethodAnnotation(RequiresAction::class.java)
        val classAnno = handler.beanType.getAnnotation(RequiresAction::class.java)

        val permission = methodAnno?.permission?.takeIf { it.isNotBlank() }
            ?: classAnno?.permission?.takeIf { it.isNotBlank() }
            ?: return null

        val action = methodAnno?.action?.takeIf { it.isNotBlank() }
            ?: classAnno?.action?.takeIf { it.isNotBlank() }
            ?: return null

        return permission to action
    }
}
