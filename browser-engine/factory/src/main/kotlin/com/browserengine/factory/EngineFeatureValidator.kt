package com.browserengine.factory

import com.browserengine.core.EngineType

fun interface EngineFeatureValidator {
    fun validate(type: EngineType): EngineFeatureValidation
}

data class EngineFeatureValidation(
    val isValid: Boolean,
    val reason: String? = null
) {
    companion object {
        fun valid(): EngineFeatureValidation = EngineFeatureValidation(isValid = true)

        fun invalid(reason: String): EngineFeatureValidation =
            EngineFeatureValidation(isValid = false, reason = reason)
    }
}
