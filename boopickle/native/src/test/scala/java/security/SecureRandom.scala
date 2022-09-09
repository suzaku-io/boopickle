package java.security

/**
  * Insecure SecureRandom implementation just for tests.
  * Scala Native doesn't provide SecureRandom since
  * https://github.com/scala-native/scala-native/issues/2600
  */
class SecureRandom extends java.util.Random
