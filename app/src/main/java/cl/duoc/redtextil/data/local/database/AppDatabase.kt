package cl.duoc.redtextil.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.duoc.redtextil.data.local.dao.ConsultaDao
import cl.duoc.redtextil.data.local.dao.CorreoDao
import cl.duoc.redtextil.data.local.dao.EmprendimientoDao
import cl.duoc.redtextil.data.local.dao.InteraccionDao
import cl.duoc.redtextil.data.local.dao.PublicacionDao
import cl.duoc.redtextil.data.local.entity.ConsultaEntity
import cl.duoc.redtextil.data.local.entity.CorreoEntity
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import cl.duoc.redtextil.data.local.entity.PublicacionEntity

@Database(
    entities = [
        EmprendimientoEntity::class,
        PublicacionEntity::class,
        ConsultaEntity::class,
        InteraccionEntity::class,
        CorreoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emprendimientoDao(): EmprendimientoDao
    abstract fun publicacionDao(): PublicacionDao
    abstract fun consultaDao(): ConsultaDao
    abstract fun interaccionDao(): InteraccionDao
    abstract fun correoDao(): CorreoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "red_textil_circular.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
