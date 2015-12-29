package udata.pubsub


class LocalPubSubManagerSpec extends PubSubManagerSpec {

  def pubSubManager(testCode:(PubSubManager[Array[Byte]]) => Any) {
    val pubSub = new LocalPubSubManager[Array[Byte]]()
    testCode(pubSub)
  }

}