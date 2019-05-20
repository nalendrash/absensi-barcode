package nanz.absen.adapter

import android.content.Context
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itextpdf.text.*
import com.itextpdf.text.pdf.FontSelector
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.android.synthetic.main.mhs_item.view.*
import nanz.absen.R
import nanz.absen.model.MahasiswaModel
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MahasiswaListAdapter(private val context: Context, private val items: List<MahasiswaModel>):
        RecyclerView.Adapter<MahasiswaListAdapter.ViewHolder>(){

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val databaseReference = FirebaseDatabase.getInstance().reference

        fun bindItem(items: MahasiswaModel, context: Context){
            itemView.tvItemNama.text = items.nama
            itemView.tvItemNBI.text = items.nbi
            itemView.tvItemDateTime.text = items.tglWaktuAbsen

            itemView.btItemDelete.setOnClickListener {
                context.alert("Hapus data?", "Hapus Data", {
                    positiveButton("Ok"){
                        databaseReference.child("mahasiswa").orderByChild("nbi").equalTo(items.nbi).addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                            }
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (nbiSnapshot in dataSnapshot.children){
                                    nbiSnapshot.ref.removeValue()
                                }
                            }
                        })
                    }
                    negativeButton("Cancel"){
                        it.dismiss()
                    }
                }).show()
            }

            itemView.btItemSave2PDF.setOnClickListener {
                val rootPath = Environment.getExternalStorageDirectory().toString()
                val absenDir = File(rootPath + "/Absen")
                absenDir.mkdir()
                val idCardDir = File(absenDir.toString() + "/ID Card")
                idCardDir.mkdir()
                val file = idCardDir.path + "/IDCard" + items.nbi?.substring(7, 10) + ".pdf"

                val doc = Document()
                PdfWriter.getInstance(doc, FileOutputStream(file))
                doc.open()

//                title
                val selectorTitle = FontSelector()
                val titleFont = FontFactory.getFont(FontFactory.HELVETICA, 24.0f)
                selectorTitle.addFont(titleFont)
                var ph = selectorTitle.process("ID Card")
                val titleParagraph = Paragraph()
                titleParagraph.tabSettings = TabSettings(130.0f)
                titleParagraph.add(Chunk.TABBING)
                titleParagraph.add(ph)
                doc.add(titleParagraph)

//                break line
                doc.add(Paragraph("\n\n"))

//                nama
                val selectorNama = FontSelector()
                val namaFont = FontFactory.getFont(FontFactory.HELVETICA, 18.0f)
                selectorNama.addFont(namaFont)
                val namaParagraph = Paragraph()
                namaParagraph.tabSettings = TabSettings(80.0f)
                namaParagraph.add(selectorNama.process("Nama"))
                namaParagraph.add(Chunk.TABBING)
                namaParagraph.add(selectorNama.process(": "+items.nama))
                doc.add(namaParagraph)

//                break line
                doc.add(Paragraph("\n"))

//                nbi
                val selectorNbi = FontSelector()
                val nbiFont = FontFactory.getFont(FontFactory.HELVETICA, 18.0f)
                selectorNbi.addFont(nbiFont)
                val nbiParagraph = Paragraph()
                nbiParagraph.tabSettings = TabSettings(80.0f)
                nbiParagraph.add(selectorNbi.process("NBI"))
                nbiParagraph.add(Chunk.TABBING)
                nbiParagraph.add(selectorNbi.process(": "+items.nbi))
                doc.add(nbiParagraph)

//                barcode
                doAsync {
                    val image = Image.getInstance(URL(items.qrCode))
                    image.scalePercent(50.0f)
                    image.setAbsolutePosition(300.0f, 580.0f)
                    doc.add(image)

                    doc.close()
                    uiThread {
                        context.alert("ID card berhasil disimpan\nDirektori : Absen\\ID Card", "ID Card tersimpan", {
                            positiveButton("Ok", {
                                it.dismiss()
                            })
                        }).show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, root: Int): MahasiswaListAdapter.ViewHolder =
            ViewHolder(LayoutInflater.from(context).inflate(R.layout.mhs_item, parent, false))

    override fun getItemCount(): Int =
            items.size

    override fun onBindViewHolder(holder: MahasiswaListAdapter.ViewHolder, position: Int) {
        holder.bindItem(items[position], context)
    }
}