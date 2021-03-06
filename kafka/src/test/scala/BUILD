FINATRA_KAFKA_CROSS = [
    {"kafka": ""},
    {"kafka": "2.5"},
]

cross_junit_tests(
    name = "scala",
    sources = [
        "com/twitter/finatra/kafka/consumers/*.scala",
        "com/twitter/finatra/kafka/serde/*.scala",
        "com/twitter/finatra/kafka/test/integration/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    cross_build = FINATRA_KAFKA_CROSS,
    strict_deps = True,
    dependencies = [
        ":test-deps{_kafka_kafka}",
        "3rdparty/jvm/org/apache/zookeeper:zookeeper-server",
        "3rdparty/jvm/org/mockito:mockito-core",
        "3rdparty/jvm/org/scalatestplus:junit",
        scoped(
            "3rdparty/jvm/org/slf4j:slf4j-simple",
            scope = "runtime",
        ),
        "finatra/inject/inject-modules/src/test/scala/com/twitter/inject/modules",
        "finatra/jackson/src/test/scala/com/twitter/finatra/json",
        "finatra/kafka/src/main/java",
        "finatra/kafka/src/test/resources",
        "finatra/kafka/src/test/thrift:thrift-scala",
    ],
)

cross_scala_library(
    name = "test-deps",
    sources = [
        "com/twitter/finatra/kafka/test/*.scala",
        "com/twitter/finatra/kafka/test/utils/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    cross_build = FINATRA_KAFKA_CROSS,
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-kafka-tests{-kafka}",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/kafka:kafka{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-clients{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test{-kafka}",
        # EmbeddedKafkaCluster.java in 2.5 has dependency on kafka-streams code.
        "3rdparty/jvm/org/apache/kafka:kafka-streams{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-test{-kafka}",
        "3rdparty/jvm/org/apache/zookeeper:zookeeper-server",
        "3rdparty/jvm/org/scalatest",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/jackson/src/test/scala/com/twitter/finatra/json",
        "finatra/kafka/src/main/scala:scala{_kafka_kafka}",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/junit",
        "3rdparty/jvm/org/apache/kafka:kafka-clients-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-clients{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-streams-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-streams{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka-test{-kafka}",
        "3rdparty/jvm/org/apache/kafka:kafka{-kafka}",
        "3rdparty/jvm/org/apache/zookeeper:zookeeper-server",
        "3rdparty/jvm/org/scalatest",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-core/src/test/scala/com/twitter/inject",
        "finatra/inject/inject-server/src/test/scala/com/twitter/inject/server",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/kafka/src/main/scala:scala{_kafka_kafka}",
    ],
)
