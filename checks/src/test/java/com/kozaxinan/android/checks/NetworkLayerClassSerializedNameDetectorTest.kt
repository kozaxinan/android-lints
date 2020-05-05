package com.kozaxinan.android.checks

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.bytes
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.kozaxinan.android.checks.NetworkLayerClassSerializedNameDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassSerializedNameDetectorTest {

  private fun retrofit(): TestFile.BinaryTestFile = bytes(
      "libs/retrofit-2.7.2.jar",
      javaClass
          .getResourceAsStream("/retrofit-2.7.2.jar")
          .readBytes()
  )

  private fun gson(): TestFile.BinaryTestFile = bytes(
      "libs/gson-2.8.6.jar",
      javaClass
          .getResourceAsStream("/gson-2.8.6.jar")
          .readBytes()
  )

  @Test
  fun `kotlin file with SerializedName`() {
    lint()
        .files(
            retrofit(),
            gson(),
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
                
                import com.google.gson.annotations.SerializedName
                
                data class Dto(
                    @SerializedName("totalResults") val totalResults: Int,
                    @SerializedName("totalNewResults") val totalNewResults: Int,
                    @SerializedName("name") val name: String,
                    @SerializedName("bool") val bool: Boolean
                ) {
                
                  companion object {

                    val EMPTY = Dto(0, 0, "", false)
                  }
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin enum file without SerializedName`() {
    lint()
        .files(
            retrofit(),
            gson(),
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

                import com.google.gson.annotations.SerializedName
                
                data class Dto(
                    @SerializedName("type") val type: PremiumType
                )
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                import com.google.gson.annotations.SerializedName

                enum class PremiumType {
                
                  @SerializedName("SCHUFA") SCHUFA,
                  ARVATO;
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [ARVATO] fields. [NetworkLayerClassSerializedNameRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin enum with fields file without SerializedName`() {
    lint()
        .files(
            retrofit(),
            gson(),
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

                import com.google.gson.annotations.SerializedName
                
                data class Dto(
                    @SerializedName("type") val type: PremiumType
                )
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                import com.google.gson.annotations.SerializedName

                enum class PremiumType(val id: Int) {
                
                  @SerializedName("SCHUFA") SCHUFA(1),
                  ARVATO(2);
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [ARVATO] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
                
                import com.google.gson.annotations.SerializedName
                
                internal data class Dto(
                    @SerializedName("totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @SerializedName("name") val name: String
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
                
                import com.google.gson.annotations.SerializedName
                
                internal data class Dto(
                    @SerializedName("totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @SerializedName("name") val name: String
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
              fun get(): Dto
                  ~~~
            src/foo/Api2.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
                
                class Dto(
                    val totalResults: Int,
                    var innerDto: InnerDto
                )
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                class InnerDto(
                    var innerResults: Int
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, innerDto, innerResults] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
            src/foo/Api.kt:9: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
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
            gson(),
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with Unit`() {
    lint()
        .files(
            retrofit(),
            gson(),
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `java file with SerializedName`() {
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
                
                import com.google.gson.annotations.SerializedName;
                
                class Dto {
                    @SerializedName("a") final int totalResults;
                    @SerializedName("b") final int totalNewResults;
                    
                    Dto(int totalResults, int totalNewResults) {
                      this.totalResult = totalResults;
                      this.totalNewResults = totalNewResults;
                    }
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `java file without SerializedName`() {
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
                
                import com.google.gson.annotations.SerializedName;
                
                import java.util.ArrayList;
                import java.util.List;
                
                class Dto {
                
                    @SerializedName("a") final int totalResults;
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
        .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
        .run()
        .expect(
            """
            src/foo/Api.java:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
              Dto get();
                  ~~~
            src/foo/Api.java:11: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields. [NetworkLayerClassSerializedNameRule]
              Dto get2();
                  ~~~~
            0 errors, 0 warnings
            """.trimIndent()
        )
  }
}
