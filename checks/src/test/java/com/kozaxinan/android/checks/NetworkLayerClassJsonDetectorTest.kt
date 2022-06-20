package com.kozaxinan.android.checks

import com.android.tools.lint.checks.BuiltinIssueRegistry
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_RULE
import org.junit.Test
import java.util.*

private val ISSUES_TO_TEST =
    arrayOf(ISSUE_NETWORK_LAYER_CLASS_JSON_RULE, ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE)

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassJsonDetectorTest : LintDetectorTest() {

    override fun getIssues(): List<Issue> {
        val issues: MutableList<Issue> = ArrayList<Issue>()
        val detectorClass: Class<out Detector?> = detectorInstance.javaClass
        // Get the list of issues from the registry and filter out others, to make sure
        // issues are properly registered
        val candidates = BuiltinIssueRegistry().issues
        for (issue: Issue in candidates) {
            if (issue.implementation.detectorClass === detectorClass) {
                issues.add(issue)
            }
        }
        return issues
    }

    override fun getDetector(): Detector {
        return NetworkLayerClassJsonDetector()
    }

    @Test
    fun `test kotlin file with Json`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo
                
                import retrofit2.http.GET
                
                internal interface Api {
                
                  @GET("url") 
                  suspend fun get(): Dto
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass
                
                @JsonClass(generateAdapter = true)
                data class Dto constructor(
                    @Json(name = "totalResults") val totalResults: Int,
                    @Json(name = "totalNewResults") @Deprecated
                    val totalNewResults: Int,
                    @Json(name = "name") val name: String,
                    @Json(name = "bool") val bools: List<Boolean>?
                ) {
                
                  companion object {

                    val EMPTY = Dto(0, 0, "", false)
                    
                    val mapping: Map<String, String> = mapOf(
                      "x" to "x",
                      "y" to "y"
                    )
                  }
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with Json that has nested objects`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation(),
                kotlin(
                    """
                package foo
                
                import retrofit2.http.GET
                
                internal interface Api {
                
                  @GET("url") 
                  suspend fun get(): Dto
                }
                """.trimIndent()
                ),
                kotlin(
                    """
                package foo
                
                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass
                
                @JsonClass(generateAdapter = true)
                data class Dto constructor(
                    @Json(name = "totalResults") val totalResults: Int,
                    @Json(name = "totalNewResults") @Deprecated
                    val totalNewResults: Int,
                    @Json(name = "name") val name: String,
                    @Json(name = "bool") val bools: List<Boolean>?,
                    @Json(name = "dto") val dtos: List<Dto>?,
                ) {
                
                  companion object {

                    val EMPTY = Dto(0, 0, "", false)
                    
                    val mapping: Map<String, String> = mapOf(
                      "x" to "x",
                      "y" to "y"
                    )
                  }
                }
                """.trimIndent()
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin enum file with JsonClass`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass
                
                @JsonClass(generateAdapter = true)
                data class Dto(
                    @Json(name = "inner") val inners: List<InnerDto>
                )

                @JsonClass(generateAdapter = true)
                data class InnerDto(
                    @Json(name = "type") val type: PremiumType
                ) {
                
                    enum class PremiumType {
                    
                      @Json(name = "SCHUFA") 
                      SCHUFA,
                      ARVATO
                    }
                }
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [PremiumType] classes [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [ARVATO] fields [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test suspending list wrapped in a response`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET
                import retrofit2.Response

                interface Api {

                  @GET("url")
                  suspend fun get(): Response<List<Dto>>
                }
                """.trimIndent()
                ),
                kotlin(
                    """
                package foo

                import com.squareup.moshi.JsonClass
                
                @JsonClass(generateAdapter = true)
                data class Dto(
                  val myEnums: List<EnumType>?
                )

                enum class EnumType {
                  FIRST,
                  SECOND
                }

                """.trimIndent()
                )
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE)
            .run()
            .expect(
                """src/foo/Api.kt:9: Information: Return type doesn't have @JsonClass annotation for [EnumType] classes [NetworkLayerClassJsonClassRule]
  suspend fun get(): Response<List<Dto>>
              ~~~
0 errors, 0 warnings""".trimIndent()
            )
    }

    @Test
    fun `test kotlin file without SerializedName`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                internal interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass

                @JsonClass(generateAdapter = true)
                internal data class Dto(
                    @Json(name = "totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @Json(name = "name") val name: String
                )
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
            src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields [NetworkLayerClassJsonRule]
              fun get(): Dto
                  ~~~
            0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file without SerializedName for suspend method`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                internal interface Api {

                  @GET("url")
                  suspend fun get(some:String, iasda: Int): Dto
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass

                @JsonClass(generateAdapter = true)
                internal data class Dto(
                    @Json(name = "totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @Json(name = "name") val name: String
                )
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields [NetworkLayerClassJsonRule]
                suspend fun get(some:String, iasda: Int): Dto
                            ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file without SerializedName from multiple interface`() {
        lint()
            .allowDuplicates()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """
                ).indented().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                interface Api2 {

                  @GET("url2")
                  fun get2(): Dto
                }
                """
                ).indented().indented(),
                kotlin(
                    """
                package foo

                class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int
                )
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [Dto] classes [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api2.kt:8: Information: Return type doesn't have @JsonClass annotation for [Dto] classes [NetworkLayerClassJsonClassRule]
                fun get2(): Dto
                    ~~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              src/foo/Api2.kt:8: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields [NetworkLayerClassJsonRule]
                fun get2(): Dto
                    ~~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file inner dto without SerializedName`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass

                @JsonClass(generateAdapter = true)
                class Dto(
                    @Json(name = "totalResults") val totalResults: Int,
                    @Json(name = "innerDto") var innerDto: InnerDto
                )
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass

                class InnerDto(
                    var innerResults: Int
                )
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [InnerDto] classes [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [innerResults] fields [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file return type generic without SerializedName`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.Call
                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Call<List<Dto>>
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo

                class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int
                )
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.kt:9: Information: Return type doesn't have @JsonClass annotation for [Dto] classes [NetworkLayerClassJsonClassRule]
                fun get(): Call<List<Dto>>
                    ~~~
              src/foo/Api.kt:9: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields [NetworkLayerClassJsonRule]
                fun get(): Call<List<Dto>>
                    ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file return type generic with Unit`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.Call
                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Call<List<Unit>>
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with Unit`() {
        lint()
            .files(
                retrofit(),
                jsonAnnotation(),
                jsonClassAnnotation().indented(),
                kotlin(
                    """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Unit
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file with Json`() {
        lint()
            .files(
                retrofit(),
                moshi(),
                java(
                    """
                package foo;

                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Dto get();
                }
                """
                ),
                java(
                    """
                package foo;

                import com.squareup.moshi.Json;
                import com.squareup.moshi.JsonClass;
                
                @JsonClass(generateAdapter = true)
                class Dto {
                    @Json(name = "a") final int totalResults;
                    @Json(name = "b") final int totalNewResults;
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file with Void`() {
        lint()
            .files(
                retrofit(),
                java(
                    """
                package foo;

                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Void get();
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file generic method with Void`() {
        lint()
            .files(
                retrofit(),
                java(
                    """
                package foo;

                import retrofit2.Call;
                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Call<Void> get();

                  @GET("url")
                  Call<Void> get2();
                }
                """
                )
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file without Json`() {
        lint()
            .files(
                retrofit(),
                moshi(),
                java(
                    """
                package foo;

                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Dto get();

                  @GET("url")
                  Dto get2();
                }
                """
                ).indented(),
                java(
                    """
                package foo;

                import com.squareup.moshi.Json;

                import java.util.ArrayList;
                import java.util.List;

                class Dto {

                    @Json(name = "a") final int totalResults;
                    int totalNewResults;

                    private static final List<Dto> list = new ArrayList<>();

                    Dto(int totalResults, int totalNewResults) {
                      this.totalResult = totalResults;
                      this.totalNewResults = totalNewResults;
                    }
                }
                """
                ).indented()
            )
            .issues(*ISSUES_TO_TEST)
            .run()
            .expect(
                """
              src/foo/Api.java:8: Information: Return type doesn't have @JsonClass annotation for [Dto] classes [NetworkLayerClassJsonClassRule]
                Dto get();
                    ~~~
              src/foo/Api.java:11: Information: Return type doesn't have @JsonClass annotation for [Dto] classes [NetworkLayerClassJsonClassRule]
                Dto get2();
                    ~~~~
              src/foo/Api.java:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields [NetworkLayerClassJsonRule]
                Dto get();
                    ~~~
              src/foo/Api.java:11: Information: Return type doesn't have @Json annotation for [totalNewResults] fields [NetworkLayerClassJsonRule]
                Dto get2();
                    ~~~~
              0 errors, 0 warnings
            """
            )
    }

    private fun jsonAnnotation(): TestFile {
        return java(
            """
          package com.squareup.moshi;

          import java.lang.annotation.Documented;
          import java.lang.annotation.Retention;
          import java.lang.annotation.ElementType;
          import java.lang.annotation.Target;

          import static java.lang.annotation.RetentionPolicy.RUNTIME;

          @Retention(RUNTIME)
          @Documented
          public @interface Json {
            String name();
          }
        """
        )
    }

    private fun jsonClassAnnotation(): TestFile {
        return java(
            """
          package com.squareup.moshi;

          import java.lang.annotation.Documented;
          import java.lang.annotation.Retention;
          import java.lang.reflect.Type;

          import static java.lang.annotation.RetentionPolicy.RUNTIME;

          @Retention(RUNTIME)
          @Documented
          public @interface JsonClass {
            boolean generateAdapter();
            String generator() default "";
          }
        """
        )
    }
}
