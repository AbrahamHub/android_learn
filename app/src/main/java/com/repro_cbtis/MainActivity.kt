package com.repro_cbtis

import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.repro_cbtis.databinding.ActivityMainBinding
import java.io.File

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    @Suppress("PrivatePropertyName")
    private val ETIQUETA_LOG = "ActividadPrincipal"

    // Objeto para manipular la interfaz de usuario
    private lateinit var binding: ActivityMainBinding

    // Para almacenar la lista de música al leerla, es lateinit porque se va a inicializar más delante
    private lateinit var listaMusica: ArrayList<Pair<String, String>>

    private var reproductor = MediaPlayer() // Objeto para reproducir música

    // Indice de la lista que indica la canción que se esta reproduciendo
    private var cancionActual = -1

    // Para saber si hay algo en reproducción o esta pausado
    private var enReproduccion = false

    // Cuando se crea la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Se asigna el objeto para manipular las vistas
        setContentView(binding.root) // Se asigna la vista principal

        // Se verifica si ya se tiene el permiso para leer los datos de la memoria externa
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // En caso de no tener el permiso, se solicita el permiso para leer los datos de la memoria externa
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        } else {
            // Si ya se tiene el permiso, se carga la lista de música
            cargarMusica()
        }

        // Configuración de los botones y las acciones
        binding.botonPlayPausa.setOnClickListener {

            if (cancionActual == -1) {
                cancionActual = 0 // Para comenzar en la primera
                reproducir()
            } else {
                if (enReproduccion) {
                    reproductor.pause()
                    enReproduccion = false
                    binding.botonPlayPausa.setImageResource(R.drawable.ic_play)
                } else {
                    reproductor.start()
                    enReproduccion = true
                    binding.botonPlayPausa.setImageResource(R.drawable.ic_pause)
                }
            }

        }

        binding.botonSiguiente.setOnClickListener {
            siguiente()
        }

        binding.botonAnterior.setOnClickListener {
            anterior()
        }
        binding.listaMusica.setOnItemClickListener { _, _, position, _ ->

            // Se asigna la canción y se comienza la reproducción
            cancionActual = position
            binding.botonPlayPausa.setImageResource(R.drawable.ic_pause)
            reproducir()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Se verifica si se concedio el permiso
        if (requestCode == 0 && permissions.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cargarMusica() // Se carga la lista de música
        } else {
            // Se muestra un diálogo y se cierra la app
            MaterialAlertDialogBuilder(this@MainActivity)
                .setCancelable(false) // No se puede cerrar hasta que el usuario presione aceptar
                .setMessage(getString(R.string.permiso_requerido)) // Mensaje
                .setPositiveButton(resources.getString(R.string.aceptar)) { _, _ ->
                    finishAffinity() // Se finaliza la app cuando el usuario selecciona aceptar
                }
                .show()
        }

    }

    private fun cargarMusica() {
        // Se carga la lista de música
        val listaTmp = leerListaMusica(Environment.getExternalStorageDirectory().absolutePath)

        // Se asigna la lista, si no es null se asigna como esta, si es null se deja en lista vacía
        listaMusica = listaTmp ?: arrayListOf()

        Log.i(ETIQUETA_LOG, listaMusica.toString()) // Para ver en el logcat

        if (listaMusica.size == 0) {
            // No hay música, se muestra mensaje y se deshabilitan los controles
            binding.mensajeSinMusica.visibility = View.VISIBLE
            binding.botonAnterior.isEnabled = false // Se deshabilita
            binding.botonAnterior.alpha = 0.5f // Se baja la opacidad
            binding.botonPlayPausa.isEnabled = false // Se deshabilita
            binding.botonPlayPausa.alpha = 0.5f // Se baja la opacidad
            binding.botonSiguiente.isEnabled = false // Se deshabilita
            binding.botonSiguiente.alpha = 0.5f // Se baja la opacidad
        } else {
            // Si hay música, se oculta el mensaje y se muestra la lista de música
            binding.mensajeSinMusica.visibility = View.GONE

            listaMusica.sortBy { it.second } // Se ordena la lista según el nombre de la canción

            val nombres = arrayListOf<String>() // Arreglo de nombres
            listaMusica.forEach { nombres.add(it.first) } // Se pasan los nombres al arreglo para mostrarlos
            // Se crea el adaptador con los nombres de las canciones
            val adaptador = ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, nombres)
            binding.listaMusica.adapter = adaptador

        }
    }
    /**
     *
     * ** EJEMPLO DE DOCUMENTACIÓN **
     * Funcion recursiva para leer toda la música deldispositivo en formato mp3
     * @param rutaRaiz Ruta donde se comienzan a leer los archivos
     * @return Lista con los archivos de música encontrados, o null si no hay archivos
     * */
    private fun leerListaMusica(rutaRaiz: String): ArrayList<Pair<String, String>>? {
        Log.i(ETIQUETA_LOG, rutaRaiz)
        // Lista para los archivos tipo pair, en el primer valor se almacenará la ruta absoluta y en el segundo el nombre
        val listaArchivos: ArrayList<Pair<String, String>> = ArrayList() // Se inicializa
        return try {
            val carpetaRaiz = File(rutaRaiz) // Se crea archivo abstracto de la ruta

            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            val files: Array<File> = carpetaRaiz.listFiles() // Se obtiene una lista de archivos
            Log.i(ETIQUETA_LOG, carpetaRaiz.listFiles()!!.toString())
            // Se iteran todos los archivos
            for (file in files) {
                // Si el archivo es un directorio
                if (file.isDirectory) {
                    // Se llama a la funcion de manera recursiva para cargar la lista de archivos del directorio
                    val lista = leerListaMusica(file.absolutePath)
                    if (lista != null) {
                        // Si la lista no es null, se agrega a la lista fr archivos
                        listaArchivos.addAll(lista)
                    } else {
                        break // Ya no hay contenido
                    }
                } else if (file.name.endsWith(".mp3")) { // Si tiene la extension adecuada
                    // Se agrega el archivo a la lista de archivos
                    listaArchivos.add(Pair(file.name, file.absolutePath))
                }
            }
            listaArchivos // Al final se devuelve la lista de archivos
        } catch (e: Exception) {
            Log.i(ETIQUETA_LOG,e.toString())
            // Si no se puede, devuelve null
            null
        }
    }
    private fun reproducir() {

        binding.botonPlayPausa.setImageResource(R.drawable.ic_pause) // Ajusta el ícono
        reproductor.release() // Para detener la reprodicción
        reproductor = MediaPlayer() // Se recrea la instancia
        reproductor.setAudioStreamType(AudioManager.STREAM_MUSIC) // Tipo de audio
        reproductor.setDataSource(this@MainActivity, Uri.parse(listaMusica[cancionActual].second)) // Se le indica el archivo a reproducir
        reproductor.prepare() // Prepara el objeto
        reproductor.start() // Comienza la reproducción
        enReproduccion = true
        binding.cancionActual.text = listaMusica[cancionActual].first

        // Cuando se termina de reproducir la canción, pasa a la siguiente
        reproductor.setOnCompletionListener {
            siguiente()
        }

    }

    // Pasa a la siguiente canción
    private fun siguiente() {

        // Si no hay nada en reproducción
        if (cancionActual == -1) {
            cancionActual = 0
            reproducir()
            return
        }

        // Verifica si hay siguiente canción, sino regresa al principio
        if(cancionActual < listaMusica.size -1) {
            cancionActual++
            reproducir()
        } else {
            cancionActual = 0
            reproducir()
        }

    }

    // Pasa a la canción anterior
    private fun anterior() {

        // Si no hay nada en reproducción
        if (cancionActual == -1) {
            cancionActual = 0
            reproducir()
            return
        }

        // Verifica si hay canción anterior o regresa a la última
        if(cancionActual > 0) {
            cancionActual--
            reproducir()
        } else {
            cancionActual = listaMusica.size - 1
            reproducir()
        }

    }

}