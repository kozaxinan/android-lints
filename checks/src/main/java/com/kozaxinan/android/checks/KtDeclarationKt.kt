package com.kozaxinan.android.checks

import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType

internal fun KtDeclaration.resolve(): DeclarationDescriptor? =
    LightClassGenerationSupport.getInstance(project).resolveToDescriptor(this)

internal fun KtDeclaration.getKotlinType(): KotlinType? {
    return when (val descriptor = resolve()) {
        is ValueDescriptor -> descriptor.type
        is CallableDescriptor -> if (descriptor is FunctionDescriptor && descriptor.isSuspend)
            descriptor.module.builtIns.nullableAnyType else descriptor.returnType

        else -> null
    }
}

internal fun KtLightMember<PsiMember>.kotlinTypeName(): String? =
    kotlinOrigin?.getKotlinType()?.nameIfStandardType?.asString()
