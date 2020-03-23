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
          .filterNot(::hasSerializedNameAnnotation)

      if (nonFinalFields.isNotEmpty()) {
        reportIfNotFinal(nonFinalFields, node)
      }
    }

    private fun hasSerializedNameAnnotation(field: UField): Boolean {
      return context
          .evaluator
          .getAllAnnotations(field as UAnnotated, true)
          .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
          .any { it.endsWith("SerializedName") }
    }

    private fun reportIfNotFinal(fields: List<UField>, method: UMethod) {
      val fieldsText = fields.map { it.name }
      context.report(
          issue = ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE,
          scopeClass = method,
          location = context.getNameLocation(method),
          message = "Return type doesn't have @SerializedName annotation for $fieldsText fields."
      )
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
