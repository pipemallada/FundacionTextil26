package cl.duoc.redtextil.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cl.duoc.redtextil.data.local.entity.EmprendimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmprendimientoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(emprendimiento: EmprendimientoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarLote(lista: List<EmprendimientoEntity>): List<Long>

    @Update
    suspend fun actualizar(emprendimiento: EmprendimientoEntity)

    @Query("SELECT * FROM emprendimientos WHERE id = :id")
    fun observarPorId(id: Long): Flow<EmprendimientoEntity?>

    @Query("SELECT * FROM emprendimientos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): EmprendimientoEntity?

    @Query("SELECT * FROM emprendimientos WHERE origen != 'HISTORICO' ORDER BY origen ASC, nombreComercial COLLATE NOCASE ASC")
    fun observarCuentasLocales(): Flow<List<EmprendimientoEntity>>

    @Query(
        """
        SELECT * FROM emprendimientos
        WHERE estado = 'ACTIVA'
          AND (:region = '' OR region = :region)
          AND (:comuna = '' OR comuna = :comuna)
          AND (:rubro = '' OR rubroPrincipal = :rubro OR rubroSecundario = :rubro)
          AND (:texto = ''
               OR nombreComercial LIKE '%' || :texto || '%'
               OR nombreEmprendedora LIKE '%' || :texto || '%'
               OR descripcion LIKE '%' || :texto || '%'
               OR tecnicas LIKE '%' || :texto || '%'
               OR comuna LIKE '%' || :texto || '%')
        ORDER BY CASE WHEN origen = 'HISTORICO' THEN 1 ELSE 0 END, nombreComercial COLLATE NOCASE ASC
        """
    )
    fun buscarActivas(texto: String, region: String, comuna: String, rubro: String): Flow<List<EmprendimientoEntity>>

    @Query(
        """
        SELECT * FROM emprendimientos
        WHERE (:estado = '' OR estado = :estado)
          AND (:texto = ''
               OR nombreComercial LIKE '%' || :texto || '%'
               OR nombreEmprendedora LIKE '%' || :texto || '%'
               OR comuna LIKE '%' || :texto || '%')
        ORDER BY CASE estado WHEN 'PENDIENTE' THEN 0 WHEN 'OBSERVADA' THEN 1 WHEN 'ACTIVA' THEN 2 ELSE 3 END,
                 fechaActualizacion DESC
        """
    )
    fun observarParaAdmin(estado: String, texto: String): Flow<List<EmprendimientoEntity>>

    @Query("SELECT estado AS clave, COUNT(*) AS total FROM emprendimientos GROUP BY estado")
    fun contarPorEstado(): Flow<List<ConteoPorClave>>

    @Query("SELECT COUNT(*) FROM emprendimientos WHERE origen = 'HISTORICO'")
    fun observarTotalHistoricos(): Flow<Int>

    @Query("SELECT COUNT(*) FROM emprendimientos")
    suspend fun contar(): Int

    @Query("SELECT * FROM emprendimientos WHERE rut = :rut LIMIT 1")
    suspend fun buscarPorRut(rut: String): EmprendimientoEntity?

    @Query("SELECT * FROM emprendimientos WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): EmprendimientoEntity?

    @Query("SELECT * FROM emprendimientos WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    suspend fun pendientesDeSincronizar(): List<EmprendimientoEntity>

    @Query("SELECT COUNT(*) FROM emprendimientos WHERE estadoSync = 'PENDIENTE_SINCRONIZAR'")
    fun contarPendientes(): Flow<Int>

    // Solo marcamos como sincronizada si nadie la edito mientras se enviaba
    @Query(
        """
        UPDATE emprendimientos SET estadoSync = 'SINCRONIZADO', remoteId = :remoteId
        WHERE id = :id AND fechaActualizacion = :version
        """
    )
    suspend fun marcarSincronizado(id: Long, remoteId: Long?, version: Long)
}
