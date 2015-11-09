package serializables

import spray.json.DefaultJsonProtocol

/**
 * Created by eli on 09/11/15.
 */
case class HumanInteraction(status:String,body:String,created_on:String)
case class StatusMsg(status:String,last_updated:String)
case class StatusHistory(status:StatusMsg,hmInteraction:Seq[HumanInteraction])

object Implicits extends DefaultJsonProtocol {
  implicit val hmInteractionFormat = jsonFormat3(HumanInteraction)
  implicit val stMsgFormat = jsonFormat2(StatusMsg)
  implicit val stHistFormat = jsonFormat2(StatusHistory)
}
