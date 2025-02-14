package com.tencent.bkrepo.common.service.util

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention
@Constraint(validatedBy = [ListPatternValidator::class])
annotation class ListPattern(val message: String = "Invalid input",
                             val regexp: String,
                             val groups: Array<KClass<*>> = [],
                             val payload: Array<KClass<out Payload>> = [])

class ListPatternValidator : ConstraintValidator<ListPattern, List<String>> {

    var pattern: String? = null

    override fun initialize(constraintAnnotation: ListPattern) {
        pattern = constraintAnnotation.regexp
    }

    override fun isValid(values: List<String>, context: ConstraintValidatorContext): Boolean {
        val regex = pattern?.toRegex() ?: return false
        return values.all { regex.matches(it) }
    }
}
