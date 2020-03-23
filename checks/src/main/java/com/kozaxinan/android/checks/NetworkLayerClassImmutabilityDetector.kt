package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.ERROR
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

@Suppress("UnstableApiUsage")
class NetworkLayerClassImmutabilityDetector : Detector(), UastScanner {

  override fun getApplicableUastTypes(): List<Class<UMethod>> = listOf(UMethod::class.java)

  override fun createUastHandler(context: JavaContext): NetworkLayerDtoFieldVisitor =
      NetworkLayerDtoFieldVisitor(context)

  class NetworkLayerDtoFieldVisitor(private val context: JavaContext) : UElementHandler() {

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

    override fun visitMethod(node: UMethod) {
      if (node.containingClass?.isInterface == false) return

      val nonFinalFields = listOf(node)
          .filter(::hasRetrofitAnnotation)
          .mapNotNull { it.returnType }
          .filterIsInstance<PsiClassReferenceType>()
          .filterNot(::isUnitOrVoid)
          .map(::findInnerGenericClassType)
          .flatMap(::findAllInnerFields)
          .filterNot { it.hasModifierProperty(PsiModifier.FINAL) }

      if (nonFinalFields.isNotEmpty()) {
        reportIfNotFinal(nonFinalFields, node)
      }
    }

    private fun isUnitOrVoid(it: PsiClassReferenceType) =
        it.canonicalText.contains("Unit") || it.canonicalText.contains("Void")

    private fun hasRetrofitAnnotation(it: UMethod): Boolean {
      return context
          .evaluator
          .getAllAnnotations(it as UAnnotated, true)
          .map { uAnnotation -> uAnnotation.qualifiedName }
          .intersect(listOfRetrofitAnnotations)
          .isNotEmpty()
    }

    private fun findAllInnerFields(typeRef: PsiClassReferenceType): List<UField> {
      val typeClass = typeRef
          .resolve()
          .toUElement() as UClass

      val innerFields: List<UField> = typeClass
          .fields
          .filterNot { it.isStatic }

      return innerFields + innerFields
          .map { it.type }
          .filterIsInstance<PsiClassReferenceType>()
          .map(::findAllInnerFields)
          .flatten()
    }

    private fun findInnerGenericClassType(returnType: PsiClassReferenceType): PsiClassReferenceType {
      val substitutor = returnType.resolveGenerics()
          .substitutor
      return if (substitutor == PsiSubstitutor.EMPTY) {
        returnType
      } else {
        findInnerGenericClassType(substitutor.substitutionMap.values.first() as PsiClassReferenceType)
      }
    }

    // Move this to inner detector. After finding all the dto classes, create a new detector and report for them
    private fun reportIfNotFinal(fields: List<UField>, method: UMethod) {
      val fieldsText = fields.map { it.name }
      context.report(
          issue = ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE,
          scopeClass = method,
          location = context.getNameLocation(method),
          message = "Return type is not immutable. $fieldsText need to be final or val."
      )
    }
  }

  companion object {

    val ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE: Issue = Issue.create(
        id = "NetworkLayerImmutableDTORule",
        briefDescription = "Network layer data class' fields need to be final or val.",
        explanation = "Data classes used in network layer should be immutable by design.",
        category = CORRECTNESS,
        priority = 8,
        severity = ERROR,
        implementation = Implementation(NetworkLayerClassImmutabilityDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )
  }
}
