package com.kozaxinan.android.checks

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.bytes
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE
import com.kozaxinan.android.checks.NetworkLayerClassJsonDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_JSON_RULE
import org.junit.Test

private val ISSUES_TO_TEST = arrayOf(ISSUE_NETWORK_LAYER_CLASS_JSON_RULE, ISSUE_NETWORK_LAYER_CLASS_JSON_CLASS_RULE)

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassJsonDetectorTest {

  private fun retrofit(): TestFile.BinaryTestFile = bytes(
      "libs/retrofit-2.7.2.jar",
      javaClass
          .getResourceAsStream("/retrofit-2.7.2.jar")
          .readBytes()
  )

  @Test
  fun `kotlin file with Json`() {
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin enum file with JsonClass`() {
    lint()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass
                
                @JsonClass(generateAdapter = true)
                internal data class Dto(
                    @Json(name = "inner") val inners: List<InnerDto>
                ) {
                
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
                }
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [PsiClass:PremiumType] classes. [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [ARVATO] fields. [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file without SerializedName`() {
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
                """.trimIndent()
            ),
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
            src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields. [NetworkLayerClassJsonRule]
              fun get(): Dto
                  ~~~
            0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file without SerializedName for suspend method`() {
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
                  suspend fun get(some:String, iasda: Int): Dto
                }
                """.trimIndent()
            ),
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields. [NetworkLayerClassJsonRule]
                suspend fun get(some:String, iasda: Int): Dto
                            ~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file without SerializedName from multiple interface`() {
    lint()
        .allowDuplicates()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package foo

                import retrofit2.http.GET

                interface Api2 {

                  @GET("url2")
                  fun get2(): Dto
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package foo

                class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int
                )
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [KtLightClassImpl:class Dto(
                  val totalResults: Int,
                  val totalNewResults: Int
              )] classes. [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api2.kt:8: Information: Return type doesn't have @JsonClass annotation for [KtLightClassImpl:class Dto(
                  val totalResults: Int,
                  val totalNewResults: Int
              )] classes. [NetworkLayerClassJsonClassRule]
                fun get2(): Dto
                    ~~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              src/foo/Api2.kt:8: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassJsonRule]
                fun get2(): Dto
                    ~~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file inner dto without SerializedName`() {
    lint()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Dto
                }
                """.trimIndent()
            ),
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo

                import com.squareup.moshi.Json
                import com.squareup.moshi.JsonClass

                class InnerDto(
                    var innerResults: Int
                )
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @JsonClass annotation for [KtLightClassImpl:class InnerDto(
                  var innerResults: Int
              )] classes. [NetworkLayerClassJsonClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api.kt:8: Information: Return type doesn't have @Json annotation for [innerResults] fields. [NetworkLayerClassJsonRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file return type generic without SerializedName`() {
    lint()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.Call
                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Call<List<Dto>>
                }
                """.trimIndent()
            ),
            kotlin(
                """
                package foo

                class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int
                )
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.kt:9: Information: Return type doesn't have @JsonClass annotation for [KtLightClassImpl:class Dto(
                  val totalResults: Int,
                  val totalNewResults: Int
              )] classes. [NetworkLayerClassJsonClassRule]
                fun get(): Call<List<Dto>>
                    ~~~
              src/foo/Api.kt:9: Information: Return type doesn't have @Json annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassJsonRule]
                fun get(): Call<List<Dto>>
                    ~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file return type generic with Unit`() {
    lint()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.Call
                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Call<List<Unit>>
                }
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with Unit`() {
    lint()
        .files(
            retrofit(),
            jsonAnnotation(),
            jsonClassAnnotation(),
            kotlin(
                """
                package foo

                import retrofit2.http.GET

                interface Api {

                  @GET("url")
                  fun get(): Unit
                }
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `java file with Json`() {
    lint()
        .files(
            java(
                """
                package foo;

                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Dto get();
                }
                """.trimIndent()
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `java file with Void`() {
    lint()
        .files(
            java(
                """
                package foo;

                import retrofit2.http.GET;

                interface Api {

                  @GET("url")
                  Void get();
                }
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `java file generic method with Void`() {
    lint()
        .files(
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expectClean()
  }

  @Test
  fun `java file without Json`() {
    lint()
        .files(
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
                """.trimIndent()
            ),
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
                """.trimIndent()
            )
        )
        .issues(*ISSUES_TO_TEST)
        .run()
        .expect(
            """
              src/foo/Api.java:8: Information: Return type doesn't have @JsonClass annotation for [PsiClass:Dto] classes. [NetworkLayerClassJsonClassRule]
                Dto get();
                    ~~~
              src/foo/Api.java:11: Information: Return type doesn't have @JsonClass annotation for [PsiClass:Dto] classes. [NetworkLayerClassJsonClassRule]
                Dto get2();
                    ~~~~
              src/foo/Api.java:8: Information: Return type doesn't have @Json annotation for [totalNewResults] fields. [NetworkLayerClassJsonRule]
                Dto get();
                    ~~~
              src/foo/Api.java:11: Information: Return type doesn't have @Json annotation for [totalNewResults] fields. [NetworkLayerClassJsonRule]
                Dto get2();
                    ~~~~
              0 errors, 0 warnings
            """.trimIndent()
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
        """.trimIndent()
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
        """.trimIndent()
    )
  }
}
