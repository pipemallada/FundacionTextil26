package cl.duoc.redtextil.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.redtextil.AppContainer
import cl.duoc.redtextil.RedTextilApp

object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = contenedor()
            AppViewModel(
                createSavedStateHandle(),
                c.preferencias,
                c.networkMonitor,
                c.sincronizacionRepository,
                c.sesionRepository,
                c.consultaRepository
            )
        }
        initializer {
            val c = contenedor()
            PerfilViewModel(c.sesionRepository, c.emprendimientoRepository, c.catalogoRepository, c.consultaRepository)
        }
        initializer {
            val c = contenedor()
            RegistroViewModel(c.emprendimientoRepository, c.sesionRepository)
        }
        initializer {
            val c = contenedor()
            FichaViewModel(c.sesionRepository, c.emprendimientoRepository)
        }
        initializer {
            val c = contenedor()
            CatalogoViewModel(c.sesionRepository, c.catalogoRepository)
        }
        initializer {
            val c = contenedor()
            PublicacionFormViewModel(createSavedStateHandle(), c.sesionRepository, c.catalogoRepository, c.imageCompressor)
        }
        initializer {
            val c = contenedor()
            BandejaViewModel(c.sesionRepository, c.consultaRepository)
        }
        initializer {
            val c = contenedor()
            DirectorioViewModel(c.emprendimientoRepository, c.catalogoRepository, c.metricasRepository)
        }
        initializer {
            val c = contenedor()
            DetalleViewModel(createSavedStateHandle(), c.emprendimientoRepository, c.catalogoRepository, c.metricasRepository)
        }
        initializer {
            val c = contenedor()
            ConsultaFormViewModel(createSavedStateHandle(), c.emprendimientoRepository, c.consultaRepository)
        }
        initializer {
            MisConsultasViewModel(contenedor().consultaRepository)
        }
        initializer {
            val c = contenedor()
            AdminFichasViewModel(c.emprendimientoRepository, c.historicoRepository)
        }
        initializer {
            MetricasViewModel(contenedor().metricasRepository)
        }
        initializer {
            CorreosViewModel(contenedor().correoRepository)
        }
    }
}

private fun CreationExtras.contenedor(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RedTextilApp).container
