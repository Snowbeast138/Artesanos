package com.example.atresanosapp.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.atresanosapp.R
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.data.model.Usuario
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfHelper {

    fun generateAndShareInvoice(
        context: Context,
        pedido: Pedido,
        cliente: Usuario,
        shareMethod: String // "WhatsApp" or "Email"
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        var currentY = 50f

        // Draw Logo (if exists)
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_artesanos)
            if (bitmap != null) {
                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 80, 80, false)
                canvas.drawBitmap(scaledBitmap, 50f, currentY, paint)
            }
        } catch (e: Exception) {}

        // Header Text
        paint.color = Color.parseColor("#4CAF50") // Green Primary
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Nota de Pedido", 150f, currentY + 40f, paint)
        
        currentY += 100f

        // Divider
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(50f, currentY, 545f, currentY, paint)
        currentY += 30f

        // Info Section
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val dateStr = pedido.fechaHoraMaximaEntrega?.toDate()?.let { date ->
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(date)
        } ?: "No especificada"

        canvas.drawText("Pedido ID: #${pedido.id.takeLast(6).uppercase()}", 50f, currentY, paint)
        canvas.drawText("Fecha de Entrega: $dateStr", 300f, currentY, paint)
        currentY += 35f

        canvas.drawText("Cliente: ${cliente.nombre}", 50f, currentY, paint)
        currentY += 30f
        canvas.drawText("Teléfono: ${cliente.telefono}", 50f, currentY, paint)
        currentY += 35f

        canvas.drawText("Estado del Pedido: ${pedido.estado.name}", 50f, currentY, paint)
        currentY += 50f

        // Table Header
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawRect(50f, currentY - 20f, 545f, currentY + 15f, paint)
        
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Cant.", 60f, currentY, paint)
        canvas.drawText("Producto", 120f, currentY, paint)
        canvas.drawText("Entregado", 350f, currentY, paint)
        canvas.drawText("Precio", 450f, currentY, paint)
        currentY += 40f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (item in pedido.productos) {
            canvas.drawText("${item.cantidad}x", 60f, currentY, paint)
            canvas.drawText(item.nombre, 120f, currentY, paint)
            
            // Delivered status
            if (item.cantidadEntregada >= item.cantidad) paint.color = Color.parseColor("#4CAF50")
            else if (item.cantidadEntregada > 0) paint.color = Color.parseColor("#FF9800")
            else paint.color = Color.RED
            canvas.drawText("${item.cantidadEntregada}/${item.cantidad}", 350f, currentY, paint)
            
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$${String.format("%.2f", item.precioUnitario)}", 545f, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            
            currentY += 35f
        }
        
        currentY += 20f
        paint.color = Color.LTGRAY
        canvas.drawLine(50f, currentY, 545f, currentY, paint)
        currentY += 40f

        // Totals
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        
        canvas.drawText("Subtotal:", 300f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${String.format("%.2f", pedido.costoBruto)}", 545f, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 35f

        if (pedido.descuentoFijo > 0) {
            canvas.drawText("Descuento:", 300f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-$${String.format("%.2f", pedido.descuentoFijo)}", 545f, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            currentY += 35f
        }

        canvas.drawText("Total a Pagar:", 300f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${String.format("%.2f", pedido.costoNeto)}", 545f, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 35f

        paint.color = Color.parseColor("#4CAF50")
        canvas.drawText("Abonado:", 300f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${String.format("%.2f", pedido.montoAbonado)}", 545f, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 35f

        val deuda = pedido.costoNeto - pedido.montoAbonado
        paint.color = if (deuda > 0) Color.RED else Color.parseColor("#4CAF50")
        canvas.drawText("Deuda Restante:", 300f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$${String.format("%.2f", deuda)}", 545f, currentY, paint)
        paint.textAlign = Paint.Align.LEFT

        // Footer removed as requested
        pdfDocument.finishPage(page)

        // Save file
        val file = File(context.cacheDir, "pdfs")
        file.mkdirs()
        val pdfFile = File(file, "Nota_Pedido_${pedido.id.takeLast(6)}.pdf")
        
        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            sharePdfOrText(context, pdfFile, pedido, cliente, shareMethod)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sharePdfOrText(context: Context, file: File, pedido: Pedido, cliente: Usuario, method: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.applicationContext.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        if (method == "WhatsApp") {
            intent.setPackage("com.whatsapp")
            intent.putExtra("jid", "${cliente.telefono.replace(Regex("[^0-9]"), "")}@s.whatsapp.net")
        } else {
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(cliente.email))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Nota de Pedido")
            intent.putExtra(Intent.EXTRA_TEXT, "Hola ${cliente.nombre}, adjunto enviamos la nota en PDF de tu pedido.")
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar Nota"))
        } catch (e: Exception) { }
    }
}
