package udata.pubsub


class LocalPubSubManagerSpec extends PubSubManagerSpec {

  def displayName = "Local PubSub Manager"

  def pubSubManager(testCode:(PubSubManager[Array[Byte]]) => Any) {
    val pubSub = new LocalPubSubManager[Array[Byte]]()
    testCode(pubSub)
  }

}