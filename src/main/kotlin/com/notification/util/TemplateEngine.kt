package com.notification.util

import com.notification.domain.Template

object TemplateEngine {
    fun render(template: Template, data: Map<String, String>): String {
        var result = template.body

        // Simple template variable replacement: {{variableName}}
        data.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }

        return result
    }

    fun renderSubject(template: Template, data: Map<String, String>): String? {
        var result = template.subject ?: return null

        data.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }

        return result
    }
}
