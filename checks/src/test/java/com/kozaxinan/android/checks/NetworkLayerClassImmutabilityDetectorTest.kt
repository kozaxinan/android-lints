package com.kozaxinan.android.checks

import com.android.tools.lint.checks.BuiltinIssueRegistry
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.kozaxinan.android.checks.NetworkLayerClassImmutabilityDetector.Companion.ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE
import org.junit.Test
import java.util.*

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassImmutabilityDetectorTest : LintDetectorTest() {

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
        return NetworkLayerClassImmutabilityDetector()
    }

    @Test
    fun `test kotlin file with val`() {
        lint()
            .files(
                retrofit(),
                rxjava(),
                kotlin(
                    """
                package foo
                
                import java.util.List
                import retrofit2.http.GET
                
                interface Api {
                
                  @GET("url") 
                  suspend fun get(): List<Dto>
                }
                """
                ).indented(),
                kotlin(
                    """
                package foo
                
                import java.util.List
                import io.reactivex.Single

                data class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int,
                    val name: String,
                    val bool: Boolean,
                    val list: Single<Section>
                ) {
                
                  val modifiedName: String = name
                    
                  interface Section {

                    val type: Type
                    val title: String?
                    
                    enum class Type {
                   
                      AD,
                      CONTENT_AD;
                    }
                  }
                
                  companion object {

                    @JvmField
                    val EMPTY = Dto(0, 0, "", false, emptyList())
                  }
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with Completable`() {
        lint()
            .files(
                retrofit(),
                rxjava(),
                kotlin(
                    """
                package foo
                
                import io.reactivex.Completable
                import retrofit2.http.GET
                
                interface Api {
                
                  @GET("url") 
                  fun get(): Completable
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with RetrofitBody`() {
        lint()
            .files(
                retrofit(),
                rxjava(),
                kotlin(
                    """
                package foo
                
                import io.reactivex.Completable
                import retrofit2.http.GET
                
                interface Api {
                
                  @GET("url") 
                  fun get(): ResponseBody
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with val and Parcelable`() {
        lint()
            .files(
                retrofit(),
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
                
                data class Dto(
                    val totalResults: Int
                ) : Parcelable {
                
                  constructor(parcel: Parcel) : this(parcel.readInt()) {
                  }
                
                  override fun writeToParcel(parcel: Parcel, flags: Int) {
                    parcel.writeInt(totalResults)
                  }
                
                  override fun describeContents(): Int {
                    return 0
                  }
                
                  companion object CREATOR : Parcelable.Creator<Dto> {
                    override fun createFromParcel(parcel: Parcel): Dto {
                      return Dto(parcel)
                    }
                
                    override fun newArray(size: Int): Array<Dto?> {
                      return arrayOfNulls(size)
                    }
                  }
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with immutable list`() {
        lint()
            .files(
                retrofit(),
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
                
                data class Dto(
                    val list: List<String>
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with mutable list`() {
        lint()
            .files(
                retrofit(),
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
                    
                    data class Dto(
                        val list: MutableList<String>
                    )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Error: Return type contains mutable class types. [list in Dto] need to be immutable. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test java file with mutable list`() {
        lint()
            .files(
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
              
                  import java.util.List;
              
                  class Dto {
                  
                    public final List<String> list;
                  }
                """
                )
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin enum file with val`() {
        lint()
            .files(
                retrofit(),
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
                
                data class Dto(
                    val totalResults: Int,
                    val type: PremiumType,
                    val name: String
                )
                """
                ).indented().indented(),
                kotlin(
                    """
                package foo
                
                enum class PremiumType {
                  SCHUFA,
                  ARVATO;
                }
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with var`() {
        lint()
            .files(
                retrofit(),
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
                
                data class Dto(
                    val totalResults: Int,
                    var totalNewResults: Int,
                    val name: String
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file with var from multiple interface`() {
        lint()
            .allowDuplicates()
            .files(
                retrofit(),
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
                    var totalNewResults: Int
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              src/foo/Api2.kt:8: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get2(): Dto
                    ~~~~
              2 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file inner dto with var`() {
        lint()
            .files(
                retrofit(),
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:8: Error: Return type is not immutable. [innerDto in Dto, innerResults in InnerDto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file return type generic with var`() {
        lint()
            .files(
                retrofit(),
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
                    var totalNewResults: Int
                )
                """
                ).indented()
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.kt:9: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Call<List<Dto>>
                    ~~~
              1 errors, 0 warnings
            """
            )
    }

    @Test
    fun `test kotlin file return type generic with Unit`() {
        lint()
            .files(
                retrofit(),
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test kotlin file with Unit`() {
        lint()
            .files(
                retrofit(),
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file with final`() {
        lint()
            .files(
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
                
                class Dto {
                    final int totalResults;
                    final int totalNewResults;
                    
                    Dto(int totalResults, int totalNewResults) {
                      this.totalResult = totalResults;
                      this.totalNewResults = totalNewResults;
                    }
                }
                """
                )
            )
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expectClean()
    }

    @Test
    fun `test java file without final`() {
        lint()
            .files(
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
                
                import java.util.ArrayList;
                import java.util.List;
                
                class Dto {
                    final int totalResults;
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
            .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
            .run()
            .expect(
                """
              src/foo/Api.java:8: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                Dto get();
                    ~~~
              src/foo/Api.java:11: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                Dto get2();
                    ~~~~
              2 errors, 0 warnings
            """
            )
    }
}
