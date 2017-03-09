package com.twitter.inject.thrift.modules

import com.github.nscala_time.time
import com.github.nscala_time.time.DurationBuilder
import com.google.inject.Provides
import com.twitter.finagle._
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.{ClientId, MethodIfaceBuilder, ServiceIfaceBuilder, ThriftServiceIface}
import com.twitter.inject.annotations.Flag
import com.twitter.inject.conversions.duration._
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.inject.thrift.filters.ThriftClientFilterBuilder
import com.twitter.inject.thrift.modules.FilteredThriftClientModule.MaxDuration
import com.twitter.inject.thrift.{AndThenService, NonFiltered}
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.scrooge.ThriftService
import com.twitter.util.{Monitor, NullMonitor, Try, Duration => TwitterDuration}
import javax.inject.Singleton
import org.joda.time.Duration
import scala.language.implicitConversions
import scala.reflect.ClassTag

object FilteredThriftClientModule {
  val MaxDuration = Duration.millis(Long.MaxValue)
}

/**
 * Provides a [[FutureIface]] in the form of `RemoteService[Future]` for making calls to a remote service. The [[FutureIface]]
 * (e.g., RemoteService[Future]) wraps a ServiceIface in which each method is implemented in the form of a [[com.twitter.finagle.Service]]
 * typed from [[com.twitter.scrooge.ThriftMethod.Args]] to [[com.twitter.scrooge.ThriftMethod.SuccessType]].
 *
 * A [[FutureIface]] of `RemoteService[Future]` is used as this is the [[FutureIface]] type generated by Scrooge's
 * "services-per-endpoint" functionality  e.g. the result of calling `Thrift.client.newMethodIface(...)`; as opposed to
 * the functionally equivalent `RemoteService.FutureIface`.
 *
 * To provide per-method filters to the [[ServiceIface]] provide an implementation of [[filterServiceIface]] and use the provided
 * [[com.twitter.inject.thrift.filters.ThriftClientFilterBuilder]] to filter methods. E.g.,
 *
 * serviceIface.copy(
 *   fetchBlob =
 *      filters.method(FetchBlob)
 *        .withMethodLatency
 *        .withExponentialRetry(
 *          shouldRetryResponse = PossiblyRetryableExceptions,
 *          start = 50.millis,
 *          multiplier = 2,
 *          retries = 3)
 *        .withRequestLatency
 *        .withRequestTimeout(500.millis)
 *        .withConcurrencyLimit(
 *          initialPermits = 100)
 *        .filtered(new MyFilter)
 *        .filtered[MyOtherFilter]
 *        .andThen(serviceIface.fetchBlob))
 *
 * @see [[com.twitter.finagle.thrift.MethodIfaceBuilder]]
 * @see [[https://finagle.github.io/blog/2015/09/10/services-per-endpoint-in-scrooge/ Services-per-endpoint in Scrooge]]
 * @see [[http://twitter.github.io/scrooge/Finagle.html#creating-a-client Finagle Clients]]
 */
abstract class FilteredThriftClientModule[
    FutureIface <: ThriftService : ClassTag,
    ServiceIface <: ThriftServiceIface.Filterable[ServiceIface] : ClassTag](
    implicit serviceBuilder: ServiceIfaceBuilder[ServiceIface],
    methodBuilder: MethodIfaceBuilder[ServiceIface, FutureIface])
  extends TwitterModule
  with time.Implicits {

  override val frameworkModules = Seq(
    AndThenServiceModule,
    FilteredThriftClientFlagsModule)

  /**
   * Name of client for use in metrics
   */
  val label: String

  /**
   * Destination of client
   */
  val dest: String

 /**
  * Enable thrift mux for this connection.
  *
  * Note: Both server and client must have mux enabled otherwise
  * a nondescript ChannelClosedException will be seen.
  *
  * @see [[http://twitter.github.io/finagle/guide/FAQ.html?highlight=thriftmux#what-is-thriftmux What is ThriftMux?]]
  */
  protected val mux: Boolean = true

 /**
  * Use a high resolution [[com.twitter.util.Timer]] such that retries are run tighter to their schedule. Default: false.
  *
  * Note: There are performance implications to enabling.
  */
  protected val useHighResTimerForRetries = false

 /**
  * Configures the session acquisition `timeout` of this client (default: unbounded).
  *
  * @see [[com.twitter.finagle.param.ClientSessionParams#acquisitionTimeout]]
  * @see [[https://twitter.github.io/finagle/guide/Clients.html#timeouts-expiration]]
  * @return an [[org.joda.time.Duration]] which represents the acquisition timeout
  */
  protected def sessionAcquisitionTimeout: Duration = MaxDuration

 /**
  * Default [[com.twitter.finagle.service.RetryBudget]]. It is highly recommended that budgets
  * be shared between all filters that retry or re-queue requests to prevent retry storms.
  *
  * @see https://twitter.github.io/finagle/guide/Clients.html#retries
  * @return a default [[com.twitter.finagle.service.RetryBudget]]
  */
  protected def budget: Budget = Budget.default

  /**
   * Function to add a user-defined Monitor, c.t.finagle.DefaultMonitor will be installed
   * implicitly which handles all exceptions caught in stack. Exceptions aren't handled by
   * user-defined monitor propagated to the default monitor.
   *
   * NullMonitor has no influence on DefaultMonitor behavior here
   */
  protected def monitor: Monitor = NullMonitor

  /**
   * This method allows for further configuration of the client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * override def configureThriftMuxClient(client: ThriftMux.Client): ThriftMux.Client = {
   *   client
   *     .withProtocolFactory(myCustomProtocolFactory))
   *     .withStatsReceiver(someOtherScopedStatsReceiver)
   *     .withMonitor(myAwesomeMonitor)
   *     .withTracer(notTheDefaultTracer)
   *     .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   * }
   *
   * @param client - the [[com.twitter.finagle.ThriftMux.Client]] to configure.
   * @return a configured ThriftMux.Client.
   */
  protected def configureThriftMuxClient(client: ThriftMux.Client): ThriftMux.Client = {
    client
  }

  /**
   * This method allows for further configuration of the client for parameters not exposed by
   * this module or for overriding defaults provided herein, e.g.,
   *
   * override def configureNonThriftMuxClient(client: Thrift.Client): Thrift.Client = {
   *   client
   *     .withProtocolFactory(myCustomProtocolFactory))
   *     .withStatsReceiver(someOtherScopedStatsReceiver)
   *     .withMonitor(myAwesomeMonitor)
   *     .withTracer(notTheDefaultTracer)
   *     .withResponseClassifier(ThriftResponseClassifier.ThriftExceptionsAsFailures)
   * }
   *
   * In general it is recommended that users prefer to use ThriftMux if the server-side supports
   * mux connections.
   *
   * @param client - the [[com.twitter.finagle.Thrift.Client]] to configure.
   * @return a configured Thrift.Client.
   */
  protected def configureNonThriftMuxClient(client: Thrift.Client): Thrift.Client = {
    client
  }

 /**
  * Add filters to the ServiceIface. This is done by copying the [[ServiceIface]] then filtering
  * each method as desired via a [[com.twitter.inject.thrift.filters.ThriftClientFilterChain]] returned
  * from [[com.twitter.inject.thrift.filters.ThriftClientFilterBuilder.method]]. E.g.,
  *
  *      filters.method(FetchBlob)
  *        .withMethodLatency
  *        .withConstantRetry(
  *          shouldRetryResponse = PossiblyRetryableExceptions,
  *          start = 50.millis,
  *          retries = 3)
  *        .withRequestLatency
  *        .withRequestTimeout(250.millis)
  *        .withConcurrencyLimit(
  *          initialPermits = 500)
  *        .filtered(new MyFilter)
  *        .filtered[MyOtherFilter]
  *        .andThen(serviceIface.fetchBlob))
  *
  * Note: the [[com.twitter.inject.thrift.filters.ThriftClientFilterChain]] supports adding filters
  * either by instance or by type.
  *
  * Subclasses of this module MAY provide an implementation of `filterServiceIface` which filters the [[ServiceIface]]
  * per-method.
  *
  * @param serviceIface - the [[ServiceIface]] to filter per-method.
  * @param filters      - a [[com.twitter.inject.thrift.filters.ThriftClientFilterBuilder]] which can be invoked
  *                     to construct a [[com.twitter.inject.thrift.filters.ThriftClientFilterChain]] per-method.
  * @return a per-method filtered [[ServiceIface]]
  * @see [[com.twitter.inject.thrift.filters.ThriftClientFilterChain]]
  */
  protected def filterServiceIface(
    serviceIface: ServiceIface,
    filters: ThriftClientFilterBuilder): ServiceIface = serviceIface

  @Provides
  @Singleton
  final def providesClient(
    @Flag("timeout.multiplier") timeoutMultiplier: Int,
    @Flag("retry.multiplier") retryMultiplier: Int,
    @NonFiltered serviceIface: ServiceIface,
    injector: Injector,
    statsReceiver: StatsReceiver,
    andThenService: AndThenService
  ): FutureIface = {
    val filterBuilder = new ThriftClientFilterBuilder(
      timeoutMultiplier,
      retryMultiplier,
      injector,
      statsReceiver,
      label,
      budget,
      useHighResTimerForRetries,
      andThenService)

    Thrift.client.newMethodIface(
      filterServiceIface(
        serviceIface = serviceIface,
        filters = filterBuilder))
  }

  @Provides
  @NonFiltered
  @Singleton
  final def providesUnfilteredServiceIface(
    @Flag("timeout.multiplier") timeoutMultiplier: Int,
    clientId: ClientId,
    statsReceiver: StatsReceiver): ServiceIface = {
    val acquisitionTimeout = sessionAcquisitionTimeout.toTwitterDuration * timeoutMultiplier
    val clientStatsReceiver = statsReceiver.scope("clnt")

    val thriftClient =
      if (mux) {
        configureThriftMuxClient(
          ThriftMux.client
            .withSession.acquisitionTimeout(acquisitionTimeout)
            .withStatsReceiver(clientStatsReceiver)
            .withClientId(clientId)
            .withMonitor(monitor)
            .withRetryBudget(budget.retryBudget)
            .withRetryBackoff(budget.requeueBackoffs))
      } else {
        configureNonThriftMuxClient(
          Thrift.client
            .withSession.acquisitionTimeout(acquisitionTimeout)
            .withStatsReceiver(clientStatsReceiver)
            .withClientId(clientId)
            .withMonitor(monitor)
            .withRetryBudget(budget.retryBudget)
            .withRetryBackoff(budget.requeueBackoffs))
      }

    thriftClient
      .newServiceIface[ServiceIface](dest, label)
  }

  /* Common Retry Functions */

  protected val PossiblyRetryableExceptions: PartialFunction[Try[_], Boolean] =
    PossiblyRetryable.PossiblyRetryableExceptions

  /* Common Implicits */

  implicit def toTwitterDuration(duration: DurationBuilder): TwitterDuration = {
    TwitterDuration.fromMilliseconds(duration.toDuration.getMillis)
  }
}