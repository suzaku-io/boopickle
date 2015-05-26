package boopickle

object DefaultByteBufferProvider {
  def provider = new DirectByteBufferProvider
}
