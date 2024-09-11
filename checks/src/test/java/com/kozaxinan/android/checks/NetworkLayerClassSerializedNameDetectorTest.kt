package com.kozaxinan.android.checks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.kozaxinan.android.checks.NetworkLayerClassSerializedNameDetector.Companion.ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE
import org.junit.Test
import java.util.*

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassSerializedNameDetectorTest : LintDetectorTest() {

    override fun getIssues(): List<Issue> {
        val issues: MutableList<Issue> = ArrayList<Issue>()
        val detectorClass: Class<out Detector?> = detectorInstance.javaClass
        // Get the list of issues from the registry and filter out others, to make sure
        // issues are properly registered
        val candidates = IssueRegistry().issues
        for (issue: Issue in candidates) {
            if (issue.implementation.detectorClass === detectorClass) {
                issues.add(issue)
            }
        }
        return issues
    }

    override fun getDetector(): Detector {
        return NetworkLayerClassSerializedNameDetector()
    }

    @Test
    fun `test kotlin file with SerializedName`() {
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
                """
                ).indented(),
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
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin enum file without SerializedName`() {
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
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.google.gson.annotations.SerializedName
                
                data class Dto(
                    @SerializedName("type") val type: PremiumType
                )
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import com.google.gson.annotations.SerializedName

                enum class PremiumType {
                
                  @SerializedName("SCHUFA") SCHUFA,
                  ARVATO;
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [ARVATO] fields [NetworkLayerClassSerializedNameRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin enum with fields file without SerializedName`() {
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
                """
                ).indented(),
                kotlin(
                    """
                package foo

                import com.google.gson.annotations.SerializedName
                
                data class Dto(
                    @SerializedName("type") val type: PremiumType
                )
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import com.google.gson.annotations.SerializedName

                enum class PremiumType(val id: Int) {
                
                  @SerializedName("SCHUFA") SCHUFA(1),
                  ARVATO(2);
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [ARVATO] fields [NetworkLayerClassSerializedNameRule]
                fun get(): Dto
                    ~~~
              0 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file without SerializedName`() {
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
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import com.google.gson.annotations.SerializedName
                
                internal data class Dto(
                    @SerializedName("totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @SerializedName("name") val name: String
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields [NetworkLayerClassSerializedNameRule]
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
                gson(),
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
                
                import com.google.gson.annotations.SerializedName
                
                internal data class Dto(
                    @SerializedName("totalResults") val totalResults: Int,
                    val totalNewResults: Int,
                    @SerializedName("name") val name: String
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields [NetworkLayerClassSerializedNameRule]
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
                gson(),
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
                
                import retrofit2.http.GET
                
                interface Api2 {
                
                  @GET("url2") 
                  fun get2(): Dto
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
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields [NetworkLayerClassSerializedNameRule]
              fun get(): Dto
                  ~~~
            src/foo/Api2.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields [NetworkLayerClassSerializedNameRule]
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
                gson(),
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
                
                class Dto(
                    val totalResults: Int,
                    var innerDto: InnerDto
                )
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                class InnerDto(
                    var innerResults: Int
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
            src/foo/Api.kt:8: Information: Return type doesn't have @SerializedName annotation for [totalResults, innerDto, innerResults] fields [NetworkLayerClassSerializedNameRule]
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
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
            src/foo/Api.kt:9: Information: Return type doesn't have @SerializedName annotation for [totalResults, totalNewResults] fields [NetworkLayerClassSerializedNameRule]
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
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with Unit`() {
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
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file with SerializedName`() {
        lint()
            .files(
                gson(),
                retrofit(),
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
                
                import com.google.gson.annotations.SerializedName;
                
                class Dto {
                    @SerializedName("a") final int totalResults;
                    @SerializedName("b") final int totalNewResults;
                    
                    Dto(int totalResults, int totalNewResults) {
                      this.totalResult = totalResults;
                      this.totalNewResults = totalNewResults;
                    }
                }
                """
                )
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
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
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
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
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file without SerializedName`() {
        lint()
            .files(
                gson(),
                retrofit(),
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
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_CLASS_SERIALIZED_NAME_RULE)
            .run()
            .expect(
                """
            src/foo/Api.java:8: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields [NetworkLayerClassSerializedNameRule]
              Dto get();
                  ~~~
            src/foo/Api.java:11: Information: Return type doesn't have @SerializedName annotation for [totalNewResults] fields [NetworkLayerClassSerializedNameRule]
              Dto get2();
                  ~~~~
            0 errors, 0 warnings
            """
            )
    }
}
