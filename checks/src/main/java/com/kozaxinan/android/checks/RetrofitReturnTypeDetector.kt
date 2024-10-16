package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.kozaxinan.android.checks.RetrofitReturnTypeDetector.Visitor
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement

/**
 * Parent class for finding fields of return type of and Retrofit interface method.
 *
 * Example;
 *
 * interface Api {
 *
 *  \@GET
 *   fun restSomething(): Dto
 * }
 *
 * class Dto(val a, val b)
 *
 * for this example, [Visitor.findAllFieldsOf] will return list of UField for the fields of DTO. {a, b}
 */
@Suppress("UnstableApiUsage")
internal abstract class RetrofitReturnTypeDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes(): List<Class<UMethod>> = listOf(UMethod::class.java)

    abstract class Visitor(private val context: JavaContext) : UElementHandler() {

        private val listOfRetrofitAnnotations = listOf(
            "retrofit2.http.DELETE",
            "retrofit2.http.GET",
            "retrofit2.http.POST",
            "retrofit2.http.PUT",
            "DELETE",
            "GET",
            "POST",
            "PUT"
        )

        private val listOfRetrofitBodyAnnotations = listOf(
            "retrofit2.http.Body",
            "Body"
        )

        /**
         * Return all field of return type of a retrofit interface method.
         * Returned list is include recursive fields of complex classes and type information of genetic classes.
         *
         * Unit and Void return types are ignored.
         *
         * Static fields are ignored.
         *
         * @param node Method node to be check
         * @return A list of fields of return type of method.
         * Empty list if method doesn't belong to retrofit interface or method doesn't have valid return type.
         */
        fun findAllFieldsOf(node: UMethod): Set<UField> {
            if (node.getContainingUClass()?.isInterface != true || !hasRetrofitAnnotation(node)) return emptySet()

            val returnType = node.returnType
            return when {
                node.isSuspend() -> findAllInnerFields(node.parameters.last().type as PsiClassType)
                returnType is PsiClassType && returnType.isResponseBody() -> emptySet()
                returnType is PsiClassType && returnType.isNotUnitOrVoid() ->
                    findAllInnerFields(returnType)

                else -> emptySet()
            }
        }

        fun findAllBodyParametersOf(node: UMethod): Set<UParameter> {
            if (node.getContainingUClass()?.isInterface != true || !hasRetrofitAnnotation(node)) return emptySet()

            return node.uastParameters.filter { hasBodyAnnotation(it) }.toSet()
        }

        private fun PsiClassType.isNotUnitOrVoid() =
            !canonicalText.contains("Unit") && !canonicalText.contains("Void")

        private fun PsiClassType.isResponseBody() = canonicalText.contains("ResponseBody")

        private fun hasBodyAnnotation(parameter: UParameter): Boolean {
            return context
                .evaluator
                .getAllAnnotations(parameter as UAnnotated, true)
                .map { uAnnotation -> uAnnotation.qualifiedName }
                .intersect(listOfRetrofitBodyAnnotations)
                .isNotEmpty()
        }

        private fun hasRetrofitAnnotation(method: UMethod): Boolean {
            return context
                .evaluator
                .getAllAnnotations(method as UAnnotated, true)
                .map { uAnnotation -> uAnnotation.qualifiedName }
                .intersect(listOfRetrofitAnnotations)
                .isNotEmpty()
        }
    }
}
