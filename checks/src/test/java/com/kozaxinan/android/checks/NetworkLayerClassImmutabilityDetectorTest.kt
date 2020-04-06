package com.kozaxinan.android.checks

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.bytes
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.kozaxinan.android.checks.NetworkLayerClassImmutabilityDetector.Companion.ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE
import org.junit.Test

@Suppress("UnstableApiUsage")
internal class NetworkLayerClassImmutabilityDetectorTest {

  private fun Any.retrofit(): TestFile.BinaryTestFile = bytes(
      "libs/retrofit-2.7.2.jar",
      javaClass
          .getResourceAsStream("/retrofit-2.7.2.jar")
          .readBytes()
  )

  @Test
  fun `kotlin file with val`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                data class Dto(
                    val totalResults: Int,
                    val totalNewResults: Int,
                    val name: String,
                    val bool: Boolean
                ) {
                
                  companion object {

                    val EMPTY = Dto(0, 0, "", false)
                  }
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with Completable`() {
    lint()
        .files(
            retrofit(),
            kotlin(
                """
                package foo
                
                import io.reactivex.Completable
                import retrofit2.http.GET
                
                interface Api {
                
                  @GET("url") 
                  fun get(): Completable
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with val and Parcelable`() {
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
                """.trimIndent()
            ),
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
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with immutable list`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                data class Dto(
                    val list: List<String>
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with mutable list`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                data class Dto(
                    val list: MutableList<String>
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Error: Return type contains mutable class types. [list in Dto] need to be immutable. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `java file with mutable list`() {
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
                """.trimIndent()
            ),
            java(
                """
                  package foo;
              
                  import java.util.List;
              
                  class Dto {
                  
                    public final List<String> list;
                  }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin enum file with val`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                data class Dto(
                    val totalResults: Int,
                    val type: PremiumType,
                    val name: String
                )
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                enum class PremiumType {
                  SCHUFA,
                  ARVATO;
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with var`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                data class Dto(
                    val totalResults: Int,
                    var totalNewResults: Int,
                    val name: String
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file with var from multiple interface`() {
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
                    var totalNewResults: Int
                )
                """.trimIndent()
            )
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
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file inner dto with var`() {
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
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:8: Error: Return type is not immutable. [innerDto in Dto, innerResults in InnerDto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Dto
                    ~~~
              1 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file return type generic with var`() {
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
                """.trimIndent()
            ),
            kotlin(
                """
                package foo
                
                class Dto(
                    val totalResults: Int,
                    var totalNewResults: Int
                )
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expect(
            """
              src/foo/Api.kt:9: Error: Return type is not immutable. [totalNewResults in Dto] need to be final or val. [NetworkLayerImmutableClassRule]
                fun get(): Call<List<Dto>>
                    ~~~
              1 errors, 0 warnings
            """.trimIndent()
        )
  }

  @Test
  fun `kotlin file return type generic with Unit`() {
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
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `kotlin file with Unit`() {
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
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `java file with final`() {
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
                
                class Dto {
                    final int totalResults;
                    final int totalNewResults;
                    
                    Dto(int totalResults, int totalNewResults) {
                      this.totalResult = totalResults;
                      this.totalNewResults = totalNewResults;
                    }
                }
                """.trimIndent()
            )
        )
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
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
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
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
        .issues(ISSUE_NETWORK_LAYER_IMMUTABLE_CLASS_RULE)
        .run()
        .expectClean()
  }

  @Test
  fun `java file without final`() {
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
                """.trimIndent()
            )
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
            """.trimIndent()
        )
  }
}
