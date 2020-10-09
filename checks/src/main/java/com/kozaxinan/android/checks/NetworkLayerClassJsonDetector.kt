package com.kozaxinan.android.checks

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.INFORMATIONAL
import com.intellij.lang.jvm.JvmParameter
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Check retrofit interface methods return type for JsonName and Moshi's Json/JsonClass annotation.
 */
@Suppress("UnstableApiUsage")
internal class NetworkLayerClassJsonDetector : RetrofitReturnTypeDetector() {

  override fun createUastHandler(context: JavaContext) = NetworkLayerDtoFieldVisitor(context)

  class NetworkLayerDtoFieldVisitor(private val context: JavaContext) : Visitor(context) {

    override fun visitMethod(node: UMethod) {
      val allFields: List<UField> = findAllFieldsOf(node).filterNot { !it.isStatic && it.containingClass?.isEnum == true }

      val classes = allFields
          .mapNotNull { it.containingClass }
          .toSet()

      val checkedFields = allFields
          .filterNot(::hasJsonNameAnnotation)
          .map { it.name }
          .toMutableList()

      if (checkedFields.isNotEmpty()) {
          val constructorParamsWithJson: MutableList<String> = mutableListOf()

          val constructorParameter: List<JvmParameter> = classes
            .mapNotNull { it.constructors.firstOrNull()?.parameters }
            .fold(mutableListOf(), { acc, arrayOfJvmParameters -> acc.apply { addAll(arrayOfJvmParameters) } })

          constructorParameter.forEach { parameter ->
            val name = parameter.name
            if (name != null && parameter.annotations.any { annotation -> annotation.qualifiedName?.endsWith("Json") == true }) {
              constructorParamsWithJson.add(name)
            }
          }
          checkedFields -= constructorParamsWithJson
      }
      if (checkedFields.isNotEmpty()) {
        context.report(
            issue = ISSUE_NETWORK_LAYER_CLASS_JSON_RULE,
            scopeClass = node,
            location = context.getNameLocation(node),
            message = "Return type doesn't have @Json annotation for $checkedFields fields."
        )
      }

      val checkedClasses = classes.filterNot(::hasJsonClassAnnotation)

      if (checkedClasses.isNotEmpty()) {
        context.report(
            issue = ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE,
            scopeClass = node,
            location = context.getNameLocation(node),
            message = "Return type doesn't have @JsonClass annotation for $checkedClasses classes."
        )
      }
    }

    private fun hasJsonNameAnnotation(field: UField): Boolean {
      return context
        .evaluator
        .getAllAnnotations(field as UAnnotated, true)
        .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
        .any { it.endsWith("Json") }
    }

    private fun hasJsonClassAnnotation(clazz: PsiClass): Boolean {
      return clazz.annotations
          .mapNotNull { uAnnotation -> uAnnotation.qualifiedName }
          .any { it.endsWith("JsonClass") }
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
    val ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE: Issue = Issue.create(
        id = "NetworkLayerClassJsonClassRule",
        briefDescription = "Json annotated network layer class",
        explanation = "Data classes used in network layer should use JsonClass annotation for Moshi. Adding annotation prevents obfuscation errors.",
        category = CORRECTNESS,
        priority = 5,
        severity = INFORMATIONAL,
        implementation = Implementation(NetworkLayerClassJsonDetector::class.java, Scope.JAVA_FILE_SCOPE)
    )
  }
}
