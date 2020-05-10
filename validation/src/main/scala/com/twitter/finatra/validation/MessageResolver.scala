package com.twitter.finatra.validation

import java.lang.annotation.Annotation
import java.util.Properties

/**
 * To resolve error messages for the type of validation failure. May be pattern-matched
 * to customize handling of specific failures.
 *
 * Can be overridden to customize and localize error messages.
 */
class MessageResolver {

  val validationProperties: Properties = load

  //TODO: Use [T <: Annotation : Manifest] instead of clazz
  def resolve(clazz: Class[_ <: Annotation], values: Any*): String = {
    val unresolvedMessage = validationProperties.getProperty(clazz.getName)
    if (unresolvedMessage == null)
      "unable to resolve error message due to unknown validation annotation: " + clazz
    else
      unresolvedMessage.format(values: _*)
  }

  private def load: Properties = {
    val properties = new Properties()
    loadBaseProperties(properties)
    loadPropertiesFromClasspath(properties)
    properties
  }

  private def loadBaseProperties(properties: Properties): Unit = {
    properties.load(
      getClass.getResourceAsStream("/com/twitter/finatra/validation/validation.properties"))
  }

  private def loadPropertiesFromClasspath(properties: Properties): Unit = {
    val validationPropertiesUrl = getClass.getResource("/validation.properties")
    if (validationPropertiesUrl != null) {
      properties.load(validationPropertiesUrl.openStream())
    }
  }
}
