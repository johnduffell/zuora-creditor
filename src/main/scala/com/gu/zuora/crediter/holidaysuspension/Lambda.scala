package com.gu.zuora.crediter.holidaysuspension

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gu.zuora.crediter.Types.KeyValue
import com.gu.zuora.crediter.{ZuoraClientsFromEnvironment, ZuoraCreditTransferService, ZuoraExportGenerator}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

class Lambda extends RequestHandler[KeyValue, KeyValue] with LazyLogging {

  private implicit val zuoraClients = ZuoraClientsFromEnvironment
  private implicit val zuoraRestClient = zuoraClients.zuoraRestClient

  val exportGenerator = new ZuoraExportGenerator(GetNegativeHolidaySuspensionInvoices)
  val invoiceCrediter = new ZuoraCreditTransferService(CreateHolidaySuspensionCredit)

  override def handleRequest(event: KeyValue, context: Context): KeyValue = {
    val shouldScheduleReport = "true".equals(event.get("scheduleReport"))
    val exportId = event.get("creditInvoicesFromExport")
    val shouldCreditInvoices = Option(exportId).exists(_.nonEmpty)

    if (shouldScheduleReport) {
      Map("creditInvoicesFromExport" -> exportGenerator.generate().mkString).asJava
    } else if (shouldCreditInvoices) {
      Map("numberOfInvoicesCredited" -> invoiceCrediter.processExportFile(exportId).toString).asJava
    } else {
      logger.error(s"Lambda called with incorrect input data: $event")
      Map("nothingToDo" -> true.toString).asJava
    }
  }

}



