package test

import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._
import models._
import play.api.mvc.AnyContentAsEmpty
import module.Routes

class ApplicationSpec extends Specification {

  "an Application" should {
  
    "execute index" in new WithApplication() {
      val action = controllers.Application.index()
      val result = action(FakeRequest())

      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("text/html"))
      charset(result) must equalTo(Some("utf-8"))
      contentAsString(result) must contain("Hello world")
    }

    "tess custom validator failure" in new WithApplication() {
      import play.data._
       val userForm = new Form(classOf[JUser])
       val anyData = new java.util.HashMap[String,String]
       anyData.put("email", "")
       userForm.bind(anyData).errors.toString must contain("ValidationError(email,error.invalid,[class validator.NotEmpty]")
    }
    "tess custom validator passing" in new WithApplication() {
      import play.data._
       val userForm = new Form(classOf[JUser])
       val anyData = new java.util.HashMap[String,String]
       anyData.put("email", "peter.hausel@yay.com")
       userForm.bind(anyData).get.toString must contain ("")
    }
  
    "execute index again" in new WithApplication() {
      val action = controllers.Application.index()
      val result = action(FakeRequest())

      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("text/html"))
      charset(result) must equalTo(Some("utf-8"))
      contentAsString(result) must contain("Hello world")
    }
    
    "execute json" in new WithApplication() {
      val Some(result) = route(FakeRequest(GET, "/json"))
      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("application/json"))
      contentAsString(result) must contain("{\"id\":1,\"name\":\"Sadek\",\"favThings\":[\"tea\"]}")
    }

    def javaResult(result: play.api.mvc.Result) =
      new play.mvc.Result {
        def getWrappedResult = result
      }

    "execute json with content type" in new WithApplication() {
      // here we just test the case insensitivity of FakeHeaders, which is not that
      // interesting, ...
      val Some(result) = route(FakeRequest(GET, "/jsonWithContentType",
        FakeHeaders(Seq("Accept" -> Seq("application/json"))), AnyContentAsEmpty))
      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("application/json"))
      charset(result) must equalTo(None)
      contentAsString(result) must contain( """{"Accept":"application/json"}""")
      play.test.Helpers.charset(javaResult(result)) must equalTo(null)
    }


    "execute json with content type and charset" in new WithApplication() {
      val Some(result) = route(FakeRequest(GET, "/jsonWithCharset"))
      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("application/json"))
      charset(result) must equalTo(Some("utf-8"))
      play.test.Helpers.charset(javaResult(result)) must equalTo("utf-8")
    }

    "not serve asset directories" in new WithApplication() {
      val Some(result) = route(FakeRequest(GET, "/public//"))
      status(result) must equalTo (NOT_FOUND)
    }
   
    "remove cache elements" in new WithApplication() {
      import play.api.cache.Cache
      Cache.set("foo", "bar")
      Cache.get("foo") must equalTo (Some("bar"))
      Cache.remove("foo")
      Cache.get("foo") must equalTo (None)
    }

    "reverse routes containing boolean parameters" in {
      "in the query string" in {
        controllers.routes.Application.takeBool(true).url must equalTo ("/take-bool?b=true")
        controllers.routes.Application.takeBool(false).url must equalTo ("/take-bool?b=false")
      }
      "in the  path" in {
        controllers.routes.Application.takeBool2(true).url must equalTo ("/take-bool-2/true")
        controllers.routes.Application.takeBool2(false).url must equalTo ("/take-bool-2/false")
      }
    }

    "bind boolean parameters" in {
      "from the query string" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, "/take-bool?b=true"))
        contentAsString(result) must equalTo ("true")
        val Some(result2) = route(FakeRequest(GET, "/take-bool?b=false"))
        contentAsString(result2) must equalTo ("false")
        // Bind boolean values from 1 and 0 integers too
        contentAsString(route(FakeRequest(GET, "/take-bool?b=1")).get) must equalTo ("true")
        contentAsString(route(FakeRequest(GET, "/take-bool?b=0")).get) must equalTo ("false")
      }
      "from the path" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, "/take-bool-2/true"))
        contentAsString(result) must equalTo ("true")
        val Some(result2) = route(FakeRequest(GET, "/take-bool-2/false"))
        contentAsString(result2) must equalTo ("false")
        // Bind boolean values from 1 and 0 integers too
        contentAsString(route(FakeRequest(GET, "/take-bool-2/1")).get) must equalTo ("true")
        contentAsString(route(FakeRequest(GET, "/take-bool-2/0")).get) must equalTo ("false")
      }
    }

    "bind int parameters from the query string as a list" in {

      "from a list of numbers" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, controllers.routes.Application.takeList(List(1, 2, 3)).url))
        contentAsString(result) must equalTo("123")
      }
      "from a list of numbers and letters" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, "/take-list?x=1&x=a&x=2"))
        contentAsString(result) must equalTo("12")
      }
      "when there is no parameter at all" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, "/take-list"))
        contentAsString(result) must equalTo("")
      }
      "using the Java API" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, "/take-list-java?x=1&x=2&x=3"))
        contentAsString(result) must equalTo("3 elements")
      }
    }

    "return jsonp" in {
      "Scala API" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, controllers.routes.Application.jsonp("baz").url))
        contentAsString(result) must equalTo ("baz({\"foo\":\"bar\"});")
        contentType(result) must equalTo (Some("text/javascript"))
      }
      "Java API" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, controllers.routes.JavaApi.jsonpJava("baz").url))
        contentAsString(result) must equalTo("baz({\"foo\":\"bar\"});")
        contentType(result) must equalTo(Some("text/javascript"))
      }
    }

    "instantiate controllers" in {
      "Java controller instance" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, controllers.routes.JavaControllerInstance.index().url))
        contentAsString(result) must equalTo("{\"peter\":\"foo\",\"yay\":\"value\"}")
        contentType(result) must equalTo(Some("application/json"))
      }
      "Scala controller instance" in new WithApplication() {
        val Some(result) = route(FakeRequest(GET, controllers.routes.ScalaControllerInstance.index().url))
        contentAsString(result) must equalTo("{\"peter\":\"foo\",\"yay\":\"value\"}")
        contentType(result) must equalTo(Some("application/json"))
      }
    }

    "urldecode correctly parameters from path and query string" in new WithApplication() {
      val Some(result) = route(FakeRequest(GET, "/urldecode/2%2B2?q=2%2B2"))
      contentAsString(result) must contain("fromPath=2+2")
      contentAsString(result) must contain("fromQueryString=2+2")
    }

    "test Accept header mime-types" in {
      import play.api.http.HeaderNames._
      "Scala API" in new WithApplication() {
        val url = controllers.routes.Application.accept().url
        val Some(result) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html,application/xml;q=0.5"))
        contentAsString(result) must equalTo("html")

        val Some(result2) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/*"))
        contentAsString(result2) must equalTo("html")

        val Some(result3) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "application/json"))
        contentAsString(result3) must equalTo("json")
      }
      "Java API" in new WithApplication() {
        val url = controllers.routes.JavaApi.accept().url
        val Some(result) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html,application/xml;q=0.5"))
        contentAsString(result) must equalTo("html")

        val Some(result2) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/*"))
        contentAsString(result2) must equalTo("html")

        val Some(result3) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "application/json"))
        contentAsString(result3) must equalTo("json")
      }
    }

    "support all valid Java identifiers in router" in new WithApplication() {
      val Some(result) = route(FakeRequest(GET, "/ident/3"))
      status(result) must equalTo(OK)
      contentAsString(result) must_== "3"
    }

    "perform content negotiation" in {
      running(FakeApplication()) {
        val url = controllers.routes.Application.contentNegotiation().url

        val Some(result) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html"))
        contentType(result) must equalTo (Some("text/html"))

        val Some(result2) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html;q=0.5,application/*"))
        contentType(result2) must equalTo (Some("application/json"))

        val Some(result3) = route(FakeRequest(GET, url).withHeaders(ACCEPT -> "application/xml"))
        status(result3) must equalTo (406)

        val Some(result4) = route(FakeRequest(GET, url)) // No Accept header
        contentType(result4) must equalTo (Some("text/html"))
      }
    }

    "sort Accept header values according to their quality factor and specificity" in {
      import play.api.http.MediaRange
      val r1 = FakeRequest(GET, "/foo").withHeaders(ACCEPT -> "text/*, text/html, text/html;level=1, */*")
      r1.acceptedTypes must equalTo (Seq(
        MediaRange("text", "html", Some("level=1")),
        MediaRange("text", "html", None),
        MediaRange("text", "*", None),
        MediaRange("*", "*", None)
      ))
      val r2 = FakeRequest(GET, "/foo").withHeaders(ACCEPT -> "text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5")
      r2.acceptedTypes must equalTo (Seq(
        MediaRange("text", "html", Some("level=1")),
        MediaRange("text", "html", None),
        MediaRange("*", "*", None),
        MediaRange("text", "html", Some("level=2")),
        MediaRange("text", "*", None)
      ))
    }

    "sort Accept-Language header values according to their quality factor" in {
      import play.api.i18n.Lang
      val r1 = FakeRequest(GET, "/foo").withHeaders(ACCEPT_LANGUAGE -> "da, en-gb;q=0.8, en;q=0.7")
      r1.acceptLanguages must equalTo (Seq(
        Lang("da"),
        Lang("en-gb"),
        Lang("en")
      ))
    }

    "allow reverse routing of routes includes" in new WithApplication() {
      // Force the router to bootstrap the prefix
      app.routes
      controllers.module.routes.ModuleController.index().url must_== "/module/index"
    }

    "document the router" in new WithApplication() {
      // The purpose of this test is to alert anyone that changes the format of the router documentation that
      // it is being used by Swagger. So if you do change it, please let Tony Tam know at tony at wordnik dot com.
      val someRoute = app.routes.flatMap(_.documentation.find(r => r._1 == "GET" && r._2.startsWith("/read/")))
      someRoute must beSome[(String, String, String)]
      val route = someRoute.get
      route._2 must_== "/read/$name<[^/]+>"
      route._3 must startWith("controllers.JavaApi.readCookie")
    }
  }

}
