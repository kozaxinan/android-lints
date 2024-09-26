package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.JavaEvaluator
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement
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

fun hasThrowableSuperClass(uClass: UClass?): Boolean {
    var superClass: PsiClass? = uClass?.javaPsi
    while (superClass != null) {
        if (superClass.qualifiedName == "java.lang.Throwable" || superClass.qualifiedName == "kotlin.Throwable") {
            return true
        }
        superClass = superClass.superClass
    }
    return false
}

fun hasStringSuperClass(uClass: UClass?): Boolean {
    var superClass: PsiClass? = uClass?.javaPsi
    while (superClass != null) {
        if (superClass.qualifiedName == "java.lang.String" || superClass.qualifiedName == "kotlin.String") {
            return true
        }
        superClass = superClass.superClass
    }
    return false
}

fun findAllInnerFields(
    typeRef: PsiClassType,
    visitedTypes: MutableSet<PsiClassType> = mutableSetOf(),
): Set<UField> {
    val actualReturnType = findGenericClassType(typeRef)
    val typeClass: UClass = actualReturnType
        .resolve()
        .toUElement() as? UClass
        ?: return emptySet()

    if (visitedTypes.contains(actualReturnType)) {
        return setOf()
    }
    visitedTypes.add(actualReturnType)

    val innerFields: Set<UField> = typeClass
        .fields
        .filterNot(UField::hasSuperClassWithNonFinalValue)
        .filterNot { it.isStatic && it !is PsiEnumConstant }
        .toSet()

    return innerFields +
            innerFields
                .asSequence()
                .filterNot { it.isStatic }
                .map { it.type }
                .filterIsInstance<PsiClassType>()
                .filterNot { visitedTypes.contains(it) }
                .map { innerTypeRef -> findAllInnerFields(innerTypeRef, visitedTypes) }
                .flatten()
                .toSet()
}

fun UField.hasSuperClassWithNonFinalValue(): Boolean =
    hasThrowableSuperClass(getContainingUClass()) || hasStringSuperClass(getContainingUClass())

fun findGenericClassType(returnType: PsiClassType): PsiClassType {
    val substitutor: PsiSubstitutor = returnType
        .resolveGenerics()
        .substitutor
    return if (substitutor == PsiSubstitutor.EMPTY) {
        returnType
    } else {
        when (val psiType: PsiType? = substitutor.substitutionMap.values.first()) {
            is PsiClassReferenceType -> findGenericClassType(psiType)
            is PsiWildcardType -> {
                when {
                    psiType.isSuper -> {
                        findGenericClassType(psiType.superBound as PsiClassType)
                    }

                    psiType.isExtends -> {
                        findGenericClassType(psiType.extendsBound as PsiClassType)
                    }

                    else -> {
                        returnType
                    }
                }
            }

            else -> returnType
        }
    }
}

fun UMethod.isSuspend(): Boolean {
    val modifiers = modifierList as? KtLightModifierList<*>
    return modifiers?.kotlinOrigin?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: false
}
