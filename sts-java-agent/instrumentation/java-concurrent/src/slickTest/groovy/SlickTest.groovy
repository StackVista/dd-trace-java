import stackstate.trace.agent.test.AgentTestRunner
import stackstate.trace.api.STSSpanTypes
import stackstate.trace.api.STSTags
import io.opentracing.tag.Tags
import spock.lang.Shared

import static stackstate.trace.agent.test.ListWriterAssert.assertTraces

class SlickTest extends AgentTestRunner {

  @Shared
  def database = new SlickUtils()

  def "Basic statement generates spans"() {
    setup:
    def future = database.startQuery(SlickUtils.TestQuery())
    def result = database.getResults(future)

    expect:
    result == SlickUtils.TestValue()

    assertTraces(TEST_WRITER, 1) {
      trace(0, 2) {
        span(0) {
          operationName "SlickUtils.startQuery"
          serviceName "unnamed-java-app"
          resourceName "SlickUtils.startQuery"
          parent()
          errored false
          tags {
            defaultTags()
          }
        }
        span(1) {
          operationName "${SlickUtils.Driver()}.query"
          serviceName SlickUtils.Driver()
          resourceName SlickUtils.TestQuery()
          childOf span(0)
          errored false
          tags {
            "$Tags.COMPONENT.key" "java-jdbc-prepared_statement"
            "$Tags.SPAN_KIND.key" Tags.SPAN_KIND_CLIENT
            "$STSTags.SPAN_TYPE" STSSpanTypes.SQL

            "$Tags.DB_TYPE.key" SlickUtils.Driver()
            "$Tags.DB_USER.key" SlickUtils.Username()

            "db.jdbc.url" SlickUtils.Url()
            "span.origin.type" "org.h2.jdbc.JdbcPreparedStatement"

            defaultTags()
          }
        }
      }
    }
  }

  def "Concurrent requests do not throw exception"() {
    setup:
    def sleepFuture = database.startQuery(SlickUtils.SleepQuery())

    def future = database.startQuery(SlickUtils.TestQuery())
    def result = database.getResults(future)

    database.getResults(sleepFuture)

    expect:
    result == SlickUtils.TestValue()

    // Expect two traces because two queries have been run
    assertTraces(TEST_WRITER, 2) {
      trace(0, 2, {
        span(0) {}
        span(1) {}
      })
      trace(1, 2, {
        span(0) {}
        span(1) {}
      })
    }
  }
}
