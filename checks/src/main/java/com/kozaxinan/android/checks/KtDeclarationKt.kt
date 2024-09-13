package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.JavaEvaluator
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElementOfType

internal fun KtDeclaration.resolve(): DeclarationDescriptor? =
    LightClassGenerationSupport
        .getInstance(project)
        .resolveToDescriptor(this)

internal fun KtDeclaration.getKotlinType(): KotlinType? {
    return try {
        when (val descriptor = resolve()) {
            is ValueDescriptor -> descriptor.type
            is CallableDescriptor -> if (descriptor is FunctionDescriptor && descriptor.isSuspend)
                descriptor.module.builtIns.nullableAnyType else descriptor.returnType

            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

internal fun KtLightMember<PsiMember>.kotlinTypeName(): String? =
    kotlinOrigin
        ?.getKotlinType()
        ?.nameIfStandardType
        ?.asString()


fun UField.isTypeMutable(evaluator: JavaEvaluator): Boolean {
    // Trivial check for Kotlin mutable collections. See its doc for details.
    // Note this doesn't work on typealiases, which unfortunately we can't really
    // do anything about
    if (
        (sourcePsi as? KtParameter)?.text.matchesAnyOf(KnownMutableKotlinCollections)
    ) {
        return true
    }

    if ((sourcePsi as? KtParameter)?.getKotlinType()?.getKotlinTypeFqName(true)
            ?.matchesAnyOf(KnownMutableKotlinCollections) == true
    ) {
        return true
    }

    val uParamClass = type.let(evaluator::getTypeClass)?.toUElementOfType<UClass>() ?: return false

    return uParamClass.name in KnownMutableCommonTypesSimpleNames
}

/** Lint can't read "Mutable*" Kotlin collections that are compiler intrinsics. */
val KnownMutableKotlinCollections =
    sequenceOf(
        ".*MutableMap(\\s)?<.*,(\\s)?.*>\\??",
        ".*MutableSet(\\s)?<.*>\\??",
        ".*MutableList(\\s)?<.*>\\??",
        ".*MutableCollection(\\s)?<.*>\\??",
    )
        .map(::Regex)

val KnownMutableCommonTypesSimpleNames =
    setOf(
        // Set
        "MutableSet",
        "ArraySet",
        "HashSet",
        // List
        "MutableList",
        "ArrayList",
        // Array
        "SparseArray",
        "SparseArrayCompat",
        "LongSparseArray",
        "SparseBooleanArray",
        "SparseIntArray",
        // Map
        "MutableMap",
        "HashMap",
        "Hashtable",
        // Compose
        "MutableState",
        // Flow
        "MutableStateFlow",
        "MutableSharedFlow",
        // RxJava & RxRelay
        "PublishSubject",
        "BehaviorSubject",
        "ReplaySubject",
        "PublishRelay",
        "BehaviorRelay",
        "ReplayRelay",
    )

fun String?.matchesAnyOf(patterns: Sequence<Regex>): Boolean {
    if (isNullOrEmpty()) return false
    for (regex in patterns) {
        if (matches(regex)) return true
    }
    return false
}
