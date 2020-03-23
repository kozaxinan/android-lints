package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

@Suppress("UnstableApiUsage")
internal class IssueRegistry : IssueRegistry() {

  override val issues: List<Issue> = listOf(
      NetworkLayerClassImmutabilityDetector.ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE
  )

  override val api: Int = CURRENT_API

  override val minApi: Int = 1
}
