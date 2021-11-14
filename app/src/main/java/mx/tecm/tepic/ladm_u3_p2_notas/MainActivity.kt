package mx.tecm.tepic.ladm_u3_p2_notas

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log

class MainActivity : AppCompatActivity() {
    //Firebase
    var Fire_Base = FirebaseFirestore.getInstance();
    //sql local
    var baseDatos = BaseDatos(this,"basedatos",null,1)

    var listaID = ArrayList<String>()
    var datos = ArrayList<String>()
    var DATA= ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        agregarLista()
        btnInsertar.setOnClickListener {
            insertar()
        }
        btnSincronizar.setOnClickListener{
            sincronizar()
        }
        lista.setOnItemClickListener {adapterView,view, i, l ->mostrarAlertEliminarActualizar(i) }
    }

    //Metodo insertar datos de la pantalla inicial
    private fun insertar(){
        try {
            var b = baseDatos.writableDatabase
            var valores = ContentValues()

            //Agregar a la BD valores de los campos de texto
            valores.put("DESCRIPCION",EditTextDescripcion.text.toString())
            valores.put("LUGAR",EditTextLugar.text.toString())
            valores.put("FECHA",EditTextFecha.text.toString())
            valores.put("HORA",EditTextHora.text.toString())


            var res =b.insert("NOTAS",null,valores)

            if(res==-1L){
                mensaje("FALLÓ AL INSERTAR")
            }else{
                mensaje("INSERCIÓN EXITOSA")
                limpiarValores()
            }

            b.close()

        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
        agregarLista()
    }

    //Limpiar campos de texto
    private fun limpiarValores(){
        EditTextDescripcion.setText("")
        EditTextFecha.setText("")
        EditTextHora.setText("")
        EditTextLugar.setText("")
    }

    //Agregar valores a la listaView
    private fun agregarLista(){
        datos.clear()
        listaID.clear()

        try{
            var b = baseDatos.readableDatabase
            var eventos = ArrayList<String>()
            var res = b.query("NOTAS", arrayOf("*"),null,null,null,null,null)
            listaID.clear()

            if (res.moveToFirst()){
                do{
                    var cadena = "DESCRIPCION: ${res.getString(1)}\nLUGAR: ${res.getString(2)}\nFECHA :${res.getString(3)}\n" +
                            "HORA :${res.getString(4)}"
                    eventos.add(cadena)
                    datos.add(cadena)
                    listaID.add(res.getInt(0).toString())
                }while (res.moveToNext())
            }else{
                eventos.add("NO TIENES EVENTOS")
            }

            lista.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,eventos)
            this.registerForContextMenu(lista)
            b.close()

        }catch (e: SQLiteException){ mensaje("ERROR!! " + e.message!!) }
    }

    //Mostrar ventana emergente Eliminar/Actualizar
    private fun mostrarAlertEliminarActualizar(Posicion:Int){
        var idLista=listaID.get(Posicion)

        AlertDialog.Builder(this)
            .setTitle("ATENCION!!")
            .setMessage("¿Que desea hacer con \n${datos.get(Posicion)}?")
            .setPositiveButton("ELIMINIAR"){d,i->eliminar(idLista)}
            .setNeutralButton("CANCELAR"){d,i->}
            .setNegativeButton("ACTUALIZAR"){d,i->llamarVentanaAcualizar(idLista)}
            .show()
    }

    //Mensaje
    private fun mensaje(s:String){
        AlertDialog.Builder(this)
            .setTitle("ATENCIÓN!!")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->d.dismiss()}
            .show()
    }

    private fun llamarVentanaAcualizar(idLista:String){
        var ventana= Intent(this,MainActivity2::class.java)
        ventana.putExtra("id",idLista)
        mensaje(idLista)
        startActivity(ventana)
        finish()
    }
    private fun sincronizar() {
        DATA.clear()
        Fire_Base.collection("nota")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    mensaje("Error: No se pudo recuperar data desde Firebase")
                    Log.d("tag","no")
                    return@addSnapshotListener

                }
                var cadena = ""
                for (registro in querySnapshot!!) {
                    cadena = registro.id.toString()
                    DATA.add(cadena)
                }
                try {
                    var trans = baseDatos.readableDatabase
                    var respuesta =
                        trans.query("NOTAS", arrayOf("*"), null, null, null, null, null)
                    if (respuesta.moveToFirst()) {
                        do {

                            if (DATA.any {
                                    respuesta.getString(0).toString() == it
                                })
                            {
                                DATA.remove(respuesta.getString(0).toString())
                                Fire_Base.collection("nota")
                                    .document(respuesta.getString(0))
                                    .update(
                                        "DESCRIPCION", respuesta.getString(1),
                                        "LUGAR", respuesta.getString(2),
                                        "FECHA", respuesta.getString(3),
                                        "HORA", respuesta.getString(4)
                                    ).addOnSuccessListener {

                                    }.addOnFailureListener {
                                        AlertDialog.Builder(this)
                                            .setTitle("Error")
                                            .setMessage("No se pudo ACTUALIZAR\n${it.message!!}")
                                            .setPositiveButton("Ok") { d, i -> }
                                            .show()
                                    }
                            } else {
                                var datosInsertar = hashMapOf(
                                    "DESCRIPCION" to respuesta.getString(1),
                                    "FECHA" to respuesta.getString(2),
                                    "HORA" to respuesta.getString(3),
                                    "LUGAR" to respuesta.getString(4)
                                )
                                Fire_Base.collection("nota")
                                    .document("${respuesta.getString(0)}")
                                    .set(datosInsertar as Any).addOnSuccessListener {

                                    }
                                    .addOnFailureListener {
                                        mensaje("No se pudo INSERTAR:\n${it.message!!}")
                                    }
                            }
                        } while (respuesta.moveToNext())

                    } else {
                        mensaje("No hay NOTAS")
                    }
                    trans.close()
                } catch (e: SQLiteException) {
                    mensaje("ERROR: " + e.message!!)
                }
                var el = DATA.subtract(listaID)
                if (el.isEmpty()) {

                } else {
                    el.forEach {
                        Fire_Base.collection("nota")
                            .document(it)
                            .delete()
                            .addOnSuccessListener {}
                            .addOnFailureListener { mensaje("ERROR No se puedo ELIMINAR\n" + it.message!!) }
                    }
                }

            }

        Toast.makeText(this, "Sincronizacion Exitosa!", Toast.LENGTH_SHORT).show()
    }

    private fun eliminar(idEliminar:String){
        try {
            var b = baseDatos.writableDatabase
            var res = b.delete("NOTAS","ID=?",
                arrayOf(idEliminar))
            if (res == 0){
                mensaje("ERROR No se pudo eliminar")

            }else{
                mensaje("SE ELIMINÓ CON EXITO ID ${idEliminar}")
                agregarLista()
            }
            b.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
    }
}