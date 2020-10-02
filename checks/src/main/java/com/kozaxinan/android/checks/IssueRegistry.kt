package com.kozaxinan.android.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.kozaxinan.android.checks.ImmutableDataClassDetector.Companion.ISSUE_IMMUTABLE_DATA_CLASS_RULE
import com.kozaxinan.android.checks.NetworkLayerClassImmutabilityDetector.Companion.ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_RULE
import com.kozaxinan.android.checks.NetworkLayerClassSerializedNameDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE

@Suppress("UnstableApiUsage")
internal class IssueRegistry : IssueRegistry() {

  override val issues: List<Issue> = listOf(
      ISSUE_IMMUTABLE_DATA_CLASS_RULE,
      ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE,
      ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE,
      ISSUE_NETWORK_LAYER_CLASS_JSON_RULE,
      ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE
  )

  override val api: Int = CURRENT_API

  override val minApi: Int = 1
}
