package com.technogi.microservices.mxpostalcodes

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.consul.CheckOptions
import io.vertx.ext.consul.ConsulClient
import io.vertx.ext.consul.ConsulClientOptions
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.ResponseContentTypeHandler
import io.vertx.kotlin.ext.consul.ServiceOptions
import java.lang.management.ManagementFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.time.LocalTime
import java.util.*

class HttpServer : AbstractVerticle() {

    private val log = LoggerFactory.getLogger(HttpServer::class.java)

    private lateinit var consulClient: ConsulClient
    private lateinit var serviceId: String

    override fun start(startFuture: Future<Void>?) {
        log.info("Starting server")

        val data = DataLoader.loadData()

        log.info("Loaded ${data.size} registers")


        val httpServer = vertx.createHttpServer()
        val router = Router.router(vertx)

        router.route().handler(CorsHandler.create("*")
          .allowedMethods(setOf(HttpMethod.GET, HttpMethod.OPTIONS))
          .allowedHeaders(setOf("Access-Control-Allow-Origin", "Access-Control-Allow-Headers", "Origin",
            "X-Requested-With", "Content-Type", "Accept")))

        router.get()
          .handler(ResponseContentTypeHandler.create()).produces("application/json")

        router.get("/").handler { it.response().end(JsonObject().put("service", "mx postal codes").encode()) }
        router.get("/health").handler(health())
        router.get("/:postalCode").handler(getHandler(data))

        httpServer
          .requestHandler { ctx -> router.accept(ctx) }
          .listen(config().getInteger("http.port", 8080)) { server ->
              if (server.failed()) {
                  startFuture?.fail(server.cause())
              }
              InetAddress.getAllByName("").forEach { it -> println("====${it.hostAddress}") }
              log.info("Server started at port: ${httpServer.actualPort()}")
              log.info("At ${LocalTime.now()}")
              log.info("With PID: ${ManagementFactory.getRuntimeMXBean().getName()}")
              register(Inet4Address.getLocalHost().hostAddress, httpServer.actualPort())
          }

    }

    override fun stop(stopFuture: Future<Void>?) {
        log.info("Stopping server at ${LocalTime.now()}")
        consulClient.deregisterService(serviceId) { res ->
            if (res.failed()) {
                log.error("Error de-registering service $serviceId", res.cause())
                stopFuture?.fail(res.cause())
            } else {
                log.info("Service $serviceId de-registered")
                stopFuture?.complete()
            }
        }
    }

    private fun getHandler(data: Map<Int, List<Location>>) = Handler<RoutingContext> { ctx ->
        try {
            val postalCode = ctx.request().getParam("postalCode").toInt()
            if (data.containsKey(postalCode)) {
                ctx.response().end(Json.encode(data.get(postalCode)))
            } else {
                notFound(ctx.response())
            }
        } catch (e: NumberFormatException) {
            notFound(ctx.response())
        }
    }

    private fun notFound(response: HttpServerResponse) {
        response.setStatusCode(404).setStatusMessage("Postal Code not found")
          .end(JsonObject().put("status", 404).put("description", "Postal code not found").encode())
    }

    private fun health(): HealthCheckHandler {
        val healthChecks = HealthCheckHandler.create(vertx)
        healthChecks.register("general") {
            it.complete(Status.OK())
        }
        return healthChecks
    }

    private fun register(host: String, port: Int) {
        if (config().containsKey("consul"))
            registerConsul(config().getJsonObject("consul"), host, port)


    }

    private fun registerConsul(config: JsonObject, host: String, port: Int) {
        this.consulClient = ConsulClient
          .create(vertx, ConsulClientOptions()
            .setHost(config.getString("host", "localhost"))
            .setPort(config.getInteger("port", 8500)))

        val serviceName = "postal-codes-mx"
        this.serviceId = "$serviceName-${host.replace(".", "-")}_$port"
        val checkHttp = "http://${config.getJsonObject("check").getString("host")}:$port/${config.getJsonObject("check").getString("health", "")}"

        log.info("Registering check on $checkHttp")
        val options = ServiceOptions()
          .setName(serviceName)
          .setId(serviceId)
          .setTags(Arrays.asList(host, "$port"))
          .setAddress(config.getJsonObject("check").getString("host"))
          .setPort(port)
          .setCheckOptions(CheckOptions()
            .setDeregisterAfter("120s")
            .setInterval(config.getJsonObject("check").getString("interval", "10s"))
            .setHttp(checkHttp))

        log.info("Registering checkpoint at ${options.checkOptions.http}")

        consulClient.registerService(options, { event ->
            if (event.succeeded()) {
                log.info("Service registered")
            }
            if (event.failed()) {
                log.error("Server could not be registered", event.cause())
            }
        })
    }
}