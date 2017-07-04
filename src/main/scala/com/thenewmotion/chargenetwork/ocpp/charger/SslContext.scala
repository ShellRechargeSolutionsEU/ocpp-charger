package com.thenewmotion.chargenetwork.ocpp.charger

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

object SslContext {
  def apply(
    keystoreFile: String,
    keystorePassword: String
  ): SSLContext = {
    val password = keystorePassword.toCharArray
    val clientCertKeyStore = {
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
      val fileInputStream = new FileInputStream(keystoreFile)
      try {
        keyStore.load(fileInputStream, password)
      } finally {
        fileInputStream.close()
      }
      keyStore
    }

    val keyManagers = {
      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(clientCertKeyStore, password)
      keyManagerFactory.getKeyManagers
    }

    val trustManagers = {
      val trustManagerFactory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm
      )
      trustManagerFactory.init(clientCertKeyStore)
      trustManagerFactory.getTrustManagers
    }

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagers, trustManagers, null)
    context
  }
}
