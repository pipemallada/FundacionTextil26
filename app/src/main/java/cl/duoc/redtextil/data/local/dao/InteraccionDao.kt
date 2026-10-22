package cl.duoc.redtextil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.redtextil.data.local.entity.InteraccionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteraccionDao {

    @Insert
    suspend fun insertar(interaccion: InteraccionEntity): Long

    @Insert
    suspend fun insertarLote(lista: List<InteraccionEntity>)

    @Query("SELECT tipo AS clave, COUNT(*) AS total FROM interacciones GROUP BY tipo")
    fun contarPorTipo(): Flow<List<ConteoPorClave>>

    @Query(
        """
        SELECT region AS clave, COUNT(*) AS total FROM interacciones
        WHERE tipo IN ('CONSULTA', 'CLIC_WHATSAPP', 'CLIC_INSTAGRAM') AND region != ''
        GROUP BY region
        """
    )
    fun contactosPorRegion(): Flow<List<ConteoPorClave>>

    @Query(
        """
        SELECT region AS clave, COUNT(*) AS total FROM interacciones
        WHERE tipo = 'BUSQUEDA' AND region != ''
        GROUP BY region
        """
    )
    fun busquedasPorRegion(): Flow<List<ConteoPorClave>>

    @Query(
        """
        SELECT rubro AS clave, COUNT(*) AS total FROM interacciones
        WHERE tipo IN ('CONSULTA', 'CLIC_WHATSAPP', 'CLIC_INSTAGRAM') AND rubro != ''
        GROUP BY rubro
        """
    )
    fun contactosPorRubro(): Flow<List<ConteoPorClave>>

    @Query(
        """
        SELECT rubro AS clave, COUNT(*) AS total FROM interacciones
        WHERE tipo = 'BUSQUEDA' AND rubro != ''
        GROUP BY rubro
        """
    )
    fun busquedasPorRubro(): Flow<List<ConteoPorClave>>

    @Query("SELECT * FROM interacciones ORDER BY fecha DESC LIMIT :limite")
    fun observarRecientes(limite: Int): Flow<List<InteraccionEntity>>

    @Query("SELECT * FROM interacciones WHERE sincronizada = 0 ORDER BY fecha ASC LIMIT 200")
    suspend fun noSincronizadas(): List<InteraccionEntity>

    @Query("SELECT COUNT(*) FROM interacciones WHERE sincronizada = 0")
    fun contarNoSincronizadas(): Flow<Int>

    @Query("UPDATE interacciones SET sincronizada = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizadas(ids: List<Long>)
}
