package controllers

import domain.{Vendor, VendorPersistence}
import play.api.libs.json.Json.toJson
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.core.commands.LastError
import security.AsAdministrator
import utils.TokenGenerator.sha256
import utils.{ErrorMarshalling, VendorMarshalling}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Vendors extends Controller with MongoController with VendorPersistence
  with VendorMarshalling with ErrorMarshalling {

  def create = AsAdministrator(parse.json) { req =>
    req.body.validate[Request].asOpt.fold(Future(BadRequest(badRequestMsg))) { vendorReq =>
      val vendor = Vendor.fromName(vendorReq.vendor)
      persist(vendor.copy(token = sha256(vendor.token))).map {
        case le if le.ok => Created(toJson(Response(vendor._id, vendor.token, vendor.name)))
      }.recover {
        case LastError(_, _, Some(11000), _, _, _, _) => Conflict(conflictMsg(vendor.name))
        case _ => InternalServerError(internalServerErrorMsg)
      }
    }
  }

}
