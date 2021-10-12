package com.kozaxinan.android.checks

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles

fun Any.retrofit(): TestFile.BinaryTestFile = TestFiles.bytes(
    "libs/retrofit-2.9.0.jar",
    javaClass
        .getResourceAsStream("/retrofit-2.9.0.jar")
        .readBytes()
)
fun Any.rxjava(): TestFile.BinaryTestFile = TestFiles.bytes(
    "libs/rxjava-2.2.21.jar",
    javaClass
        .getResourceAsStream("/rxjava-2.2.21.jar")
        .readBytes()
)

fun Any.gson(): TestFile.BinaryTestFile = TestFiles.bytes(
    "libs/gson-2.8.8.jar",
    javaClass
        .getResourceAsStream("/gson-2.8.8.jar")
        .readBytes()
)

fun Any.moshi(): TestFile.BinaryTestFile = TestFiles.bytes(
    "libs/moshi-1.12.0.jar",
    javaClass
        .getResourceAsStream("/moshi-1.12.0.jar")
        .readBytes()
)