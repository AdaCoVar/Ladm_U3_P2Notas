package mx.tecm.tepic.ladm_u3_p2_notas

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BaseDatos(context: Context?, name:String?, factory: SQLiteDatabase.CursorFactory?, version:Int):
    SQLiteOpenHelper
    (context,name,factory,version) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE NOTAS(ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, DESCRIPCION VARCHAR(200)," +
                " LUGAR VARCHAR(200),FECHA VARCHAR(8),HORA VARCHAR(5))")
    }
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }
}