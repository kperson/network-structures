package udata.pubsub


class LocalPubSubManagerActor extends PubSubManagerActor {

  lazy val manager = new LocalPubSubManager[Array[Byte]]()

}
