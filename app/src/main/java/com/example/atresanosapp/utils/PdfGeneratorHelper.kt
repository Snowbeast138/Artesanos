package com.example.atresanosapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.atresanosapp.data.model.Pedido
import java.io.File
import java.io.FileOutputStream

object PdfGeneratorHelper {

    fun generarReciboPdf(context: Context, pedido: Pedido): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Título
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Recibo de Compra - Artesanos", 10f, 40f, paint)

        // Información del Pedido
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("ID Pedido: ${pedido.id}", 10f, 70f, paint)
        canvas.drawText("Estado: ${pedido.estado.name}", 10f, 90f, paint)
        
        var yPosition = 120f
        canvas.drawText("Productos:", 10f, yPosition, paint)
        
        pedido.productos.forEach { producto ->
            yPosition += 20f
            canvas.drawText("- ${producto.cantidad}x ${producto.nombre} ($${producto.precioUnitario})", 20f, yPosition, paint)
        }

        yPosition += 40f
        paint.isFakeBoldText = true
        canvas.drawText("Total Cobrado: $${pedido.costoBruto}", 10f, yPosition, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "recibo_${pedido.id}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }
}
