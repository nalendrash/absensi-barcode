package nanz.absen

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import nanz.absen.adapter.MahasiswaListAdapter
import nanz.absen.model.MahasiswaModel
import org.jetbrains.anko.*
import java.io.FileOutputStream
import com.itextpdf.text.pdf.PdfPCell
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var nama: EditText
    lateinit var nbi: EditText

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()
    private val storageReference: StorageReference = FirebaseStorage.getInstance().getReference()
    private val listMhs: MutableList<MahasiswaModel> = mutableListOf()
    private val mhsAdapter = MahasiswaListAdapter(this, listMhs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbarMain)

        initData()

//        fab.setOnClickListener { view ->
//            initDialog()
//        }

    }

    private fun initPDF(){
        val doc = Document()

        val date = initCurrentDate()
        var format = SimpleDateFormat("dd-MM-yyyy")
        var currentDate = format.format(date)

//        var file = Environment.getExternalStorageDirectory().path + "/ListMahasiswa.pdf"
//        create directory
        val rootPath = Environment.getExternalStorageDirectory().toString()
        val absenDir = File(rootPath + "/Absen")
        absenDir.mkdir()
        val reportDir = File(absenDir.toString() + "/Report")
        reportDir.mkdir()
        val file = reportDir.path + "/Report " + currentDate + ".pdf"

//        val urName = BaseFont.createFont("src/main/resources/fonts/gothic.ttf", "UTF-8", BaseFont.EMBEDDED)
//        val headerFont = Font(urName, 36.0f, Font.NORMAL, BaseColor.BLACK)

//        var userName = intent.extras.getString("user_name")

        PdfWriter.getInstance(doc, FileOutputStream(file))
        doc.open()
        format = SimpleDateFormat("dd MMMM yyyy")
        currentDate = format.format(date)
        val detailsTitleParagraph = Paragraph("Report " + currentDate)
        detailsTitleParagraph.alignment = Element.ALIGN_CENTER
        doc.add(detailsTitleParagraph)
        doc.add(Paragraph("\n\n"))
        doc.add(createTable())
        doc.close()
//        toast("Report Saved in Absen/Report!").duration = Toast.LENGTH_LONG
        alert("Report berhasil disimpan\nDirektori : Absen\\Report", "Report tersimpan", {
            positiveButton("Ok", {
            })
        }).show()
    }

    private fun createTable(): PdfPTable{
        val table = PdfPTable(3)
        table.setTotalWidth(500.0f)
        table.setWidths(intArrayOf(3, 2, 3))

        var cell: PdfPCell
        cell = PdfPCell(Phrase("Nama"))
        cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
        table.addCell(cell)
        cell = PdfPCell(Phrase("NBI"))
        cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
        table.addCell(cell)
        cell = PdfPCell(Phrase("Tgl Waktu Masuk"))
        cell.horizontalAlignment = com.itextpdf.text.Element.ALIGN_CENTER
        table.addCell(cell)

        for (items in listMhs){
            table.addCell(items.nama)
            table.addCell(items.nbi)
            table.addCell(items.tglWaktuAbsen)
        }

        return table
    }

    private fun initData(){

        val context = this

        pbMain.visibility = View.VISIBLE
        listMain.visibility = View.GONE

        databaseReference.child("mahasiswa").addValueEventListener(object : ValueEventListener{

            override fun onCancelled(databaseError: DatabaseError) {
                pbMain.visibility = View.GONE
                listMain.visibility = View.VISIBLE
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listMhs.clear()
                for (noteDataSnapshot in dataSnapshot.children){
                    var mhs = noteDataSnapshot.getValue(MahasiswaModel::class.java)

                    listMhs.add(mhs!!)
                }
                rvListMhs.adapter = mhsAdapter
                rvListMhs.layoutManager = LinearLayoutManager(context)

                pbMain.visibility = View.GONE
                listMain.visibility = View.VISIBLE
            }

        })
    }

    private fun initDialog(){
        alert {
            title = "Tambah Mahasiswa Baru"
            customView {
                verticalLayout(R.style.FBTheme) {
                    padding = dip(20)
                    nama = editText {
                        hint = "Nama"
                    }.lparams(width = matchParent){bottomMargin = dip(10)}
                    nbi = editText {
                        hint = "NBI"
                    }
                }
            }
            positiveButton("Save"){
                addDatatoFirebase()
            }
            negativeButton("Cancel"){
                it.dismiss()
            }
        }.show()
    }

    private fun addDatatoFirebase(){
        var id = databaseReference.child("mahasiswa").push().key

        val uri = generateBarcode(id.toString(), nbi.text.toString())

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")
        progressDialog.show()

        val storageReference2nd = storageReference.child("barcode/Barcode" + nbi.text.toString().substring(7, 10) + ".jpg")

        storageReference2nd.putFile(uri).
                continueWithTask(object : Continuation<UploadTask.TaskSnapshot, Task<Uri>>{
                    override fun then(task: Task<UploadTask.TaskSnapshot>): Task<Uri> {
                        return storageReference2nd.downloadUrl
                    }
                }).addOnCompleteListener { task ->
                    toast("Data telah ditambahkan")

                    val uri = task.result

                    databaseReference.child("mahasiswa").child(id.toString()).setValue(MahasiswaModel(
                            nama.text.toString(), nbi.text.toString(), "", uri.toString()))

                    progressDialog.dismiss()
                }

    }

    private fun generateBarcode(id: String, nbi: String): Uri{
        val multiFormatWriter = MultiFormatWriter()

//        create directory
        val rootPath = Environment.getExternalStorageDirectory().toString()
        val absenDir = File(rootPath + "/Absen")
        absenDir.mkdir()
        val barcodeDir = File(absenDir.toString() + "/Barcode")
        barcodeDir.mkdir()

        val imageName = "Barcode" + nbi.substring(7, 10) + ".jpg"

        var file = File(barcodeDir, imageName)

        try {
            val bitMatrix = multiFormatWriter.encode(id, BarcodeFormat.QR_CODE,200,200)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

//            output to ext storage
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            fos.close()
            MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, file.name, file.name)
        } catch (e: WriterException) {
            e.printStackTrace();
        }

        return Uri.fromFile(file)
    }

    private fun initCurrentDate(): Date{
        val currentDate = DateFormat.getDateTimeInstance().format(Date())
        val format = SimpleDateFormat("MMM dd, yyyy h:mm:ss a")
        val newDate = format.parse(currentDate)

        return newDate
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK){
            val id = data!!.getStringExtra("id")

            val date = initCurrentDate()
            val format = SimpleDateFormat("dd MMM yyyy, hh:mm a")
            val currentDate = format.format(date)

            databaseReference.child("mahasiswa").child(id).child("tglWaktuAbsen")
                    .setValue(currentDate)

            databaseReference.child("mahasiswa").child(id).child("nama").addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    toast(dataSnapshot.value.toString() + " berhasil di absen")
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> initPDF()
            R.id.action_absen -> startActivityForResult(Intent(this, ScanQRActivity::class.java), 1)
            R.id.action_add -> initDialog()
        }
        return return super.onOptionsItemSelected(item)
    }
}
