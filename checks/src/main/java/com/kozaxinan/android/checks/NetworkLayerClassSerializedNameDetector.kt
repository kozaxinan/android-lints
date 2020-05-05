package com.kozaxinan.android.checks

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.INFORMATIONAL
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Check retrotif interface methods return type for SerializedName annotation.
 */
@Suppress("UnstableApiUsage")
internal class NetworkLayerClassSerializedNameDetector : RetrofitReturnTypeDetector() {

  override fun createUastHandler(context: JavaContext) = NetworkLayerDtoFieldVisitor(context)

  class NetworkLayerDtoFieldVisitor(private val context: JavaContext) : Visitor(context) {

    override fun visitMethod(node: UMethod) {
      val nonFinalFields = findAllFieldsOf(node)
          .filterNot { !it.isStatic && it.containingClass?.isEnum == true }
          .filterNot(::hasSerializedNameAnnotation)
          .map { it.name }

      if (nonFinalFields.isNotEmpty()) {
        context.report(
            issue = ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE,
            scopeClass = node,
            location = context.getNameLocation(node),
            message = "Return type doesn't have @SerializedName annotation for $nonFinalFields fields."
        )
      }
    }

    private fun hasSerializedNameAnnotation(field: UField): Boolean {
      return context
          .evaluator
          .getAllAnnotations(field as UAnnotated, true)
          .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
          .any { it.endsWith("SerializedName") }
    }
  }

  companion object {

    val ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE: Issue = Issue.create(
        id = "NetworkLayerClassSerializedNameRule",
        briefDescription = "SerializedName annotated network layer class",
        explanation = "Data classes used in network layer should use SerializedName annotation for Gson. Adding annotation prevents obfuscation errors.",
        category = CORRECTNESS,
        priority = 5,
        severity = INFORMATIONAL,
        implementation = Implementation(NetworkLayerClassSerializedNameDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )
  }
}
