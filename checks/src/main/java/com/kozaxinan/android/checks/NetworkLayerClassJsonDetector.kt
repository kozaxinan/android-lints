package com.kozaxinan.android.checks

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.INFORMATIONAL
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Check retrotif interface methods return type for SerializedName annotation.
 */
@Suppress("UnstableApiUsage")
internal class NetworkLayerClassJsonDetector : RetrofitReturnTypeDetector() {

  override fun createUastHandler(context: JavaContext) = NetworkLayerDtoFieldVisitor(context)

  class NetworkLayerDtoFieldVisitor(private val context: JavaContext) : Visitor(context) {

    override fun visitMethod(node: UMethod) {
      val allFields = findAllFieldsOf(node)
          .filterNot { !it.isStatic && it.containingClass?.isEnum == true }
          .filterNot(::hasJsonNameAnnotation)
      val nonFinalFields = allFields
          .map { it.name }

      if (nonFinalFields.isNotEmpty()) {
        context.report(
            issue = ISSUE_NETWORK_LAYER_CLASS_JSON_RULE,
            scopeClass = node,
            location = context.getNameLocation(node),
            message = "Return type doesn't have @Json annotation for $nonFinalFields fields."
        )
      }

//      allFields.mapNotNull { it.containingClass }
//          .toSet()
//          .filterNot(::hasJsonClassAnnotation)
    }

    private fun hasJsonNameAnnotation(field: UField): Boolean {
      return context
          .evaluator
          .getAllAnnotations(field as UAnnotated, true)
          .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
          .any { it.endsWith("Json") }
    }

    private fun hasJsonClassAnnotation(clazz: PsiClass): Boolean {
      return context
          .evaluator
          .getAllAnnotations(clazz as UAnnotated, true)
          .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
          .any { it.endsWith("Json") }
    }
  }

  companion object {

    val ISSUE_NETWORK_LAYER_CLASS_JSON_RULE: Issue = Issue.create(
        id = "NetworkLayerClassJsonRule",
        briefDescription = "Json annotated network layer class",
        explanation = "Data classes used in network layer should use Json annotation for Moshi. Adding annotation prevents obfuscation errors.",
        category = CORRECTNESS,
        priority = 5,
        severity = INFORMATIONAL,
        implementation = Implementation(NetworkLayerClassJsonDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )
  }
}
