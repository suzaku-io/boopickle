package java.security

// dummy unsecure SecureRandom implementation for tests
// for real usage, use an implementation
// like https://github.com/lolgab/scala-native-crypto for Scala Native
// and https://github.com/scala-js/scala-js-java-securerandom for Scala.js
class SecureRandom extends java.util.Random
