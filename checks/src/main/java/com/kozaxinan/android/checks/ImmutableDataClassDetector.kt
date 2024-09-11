package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.uast.UClass

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
                val fields = node
                    .allFields
                    .filterIsInstance<KtLightField>()

                validateDataClassFields(node, fields)
            }
        }

        private fun validateDataClassFields(node: UClass, fields: List<KtLightField>) {
//            val nonFinalFieldNames = fields
//                .filterNot { it.hasModifierProperty(PsiModifier.FINAL) }
//                .map { it.name }
//            if (nonFinalFieldNames.isNotEmpty()) {
//                report(node, "$nonFinalFieldNames are var. $nonFinalFieldNames need to be val.")
//            }
//
//            val mutableFieldNames = fields
//                .filter {
//                    it.text.contains("Mutable") ||
//                            it.kotlinTypeName()?.contains("Mutable") ?: false
//                }
//                .map { it.name }
//            if (mutableFieldNames.isNotEmpty()) {
//                report(
//                    node,
//                    "Return type of $mutableFieldNames are not immutable. $mutableFieldNames need to be immutable class."
//                )
//            }

            val problematicFields = fields.filter { field ->
                !field.hasModifierProperty(PsiModifier.FINAL) ||
                        field.text.contains("Mutable") ||
                        field.kotlinTypeName()?.contains("Mutable") ?: false
            }

            if (problematicFields.isNotEmpty()) {
                val message = problematicFields.joinToString(separator = "\n") { field ->
                    if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                        "${field.name} is a var. It should be a val in a data class."
                    } else {
                        "${field.name} has a mutable type. Use an immutable type instead."
                    }
                }
                // Construct a more informative message (see suggestion 3)
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
