package com.technogi.microservices.mxpostalcodes

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.consul.ConsulClient

class ServiceRegistry : AbstractVerticle(){

    lateinit var consulClient:ConsulClient

    override fun start(startFuture: Future<Void>?) {
        consulClient = ConsulClient.create(vertx)
    }

    override fun stop(stopFuture: Future<Void>?) {
        super.stop(stopFuture)
    }
}