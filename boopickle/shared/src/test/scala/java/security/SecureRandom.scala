package java.security

// dummy unsecure SecureRandom implementation for tests
// for real usage, use an implementation
// like https://github.com/lolgab/scala-native-crypto
class SecureRandom extends java.util.Random
