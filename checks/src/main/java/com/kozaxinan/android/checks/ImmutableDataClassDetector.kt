package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.JavaEvaluator
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElementOfType

/**
 * Checks all data class' fields for var and mutable class type usage.
 */
@Suppress("UnstableApiUsage")
internal class ImmutableDataClassDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes(): List<Class<UClass>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): DataClassVisitor =
        DataClassVisitor(context)

    class DataClassVisitor(private val context: JavaContext) : UElementHandler() {

        override fun visitClass(node: UClass) {
            val containsEqualHashCode = node
                .methods
                .map { it.name }
                .containsAll(listOf("equals", "hashCode"))

            if (containsEqualHashCode) {
                val fields: List<UField> = node
                    .allFields
                    .filterNot { it.name.contains("$") }
                    .mapNotNull(PsiField::toUElementOfType)

                validateDataClassFields(node, fields, context.evaluator)
            }
        }

        private fun validateDataClassFields(
            node: UClass,
            fields: List<UField>,
            evaluator: JavaEvaluator,
        ) {
            val problematicFields = fields
                .filterNot { field: UField ->
                    hasThrowableSuperClass(field.getContainingUClass())
                }
                .filter { field: UField ->
                    !field
                        .hasModifierProperty(PsiModifier.FINAL) ||
                            field.isTypeMutable(evaluator)
                }

            if (problematicFields.isNotEmpty()) {
                val message = problematicFields.joinToString(separator = "\n") { field ->
                    if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                        "${field.name} is a var. It should be a val in a data class."
                    } else {
                        "${field.name} has a mutable type. Use an immutable type instead."
                    }
                }

                report(node, message)
            }
        }

        private fun report(node: UClass, message: String) {
            context.report(
                issue = ISSUE_IMMUTABLE_DATA_CLASS_RULE,
                scopeClass = node,
                location = context.getNameLocation(node),
                message = message
            )
        }
    }

    companion object {

        val ISSUE_IMMUTABLE_DATA_CLASS_RULE: Issue = Issue.create(
            id = "ImmutableDataClassRule",
            briefDescription = "Immutable kotlin data class",
            explanation = "Kotlin data classes should be immutable by design. Use `copy()` method when instance needs to be modified.",
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(
                ImmutableDataClassDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
