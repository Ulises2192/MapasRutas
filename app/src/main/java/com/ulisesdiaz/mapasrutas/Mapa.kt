package com.ulisesdiaz.mapasrutas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.ulisesdiaz.mapasrutas.models.ResponseModel

class Mapa(mMap: GoogleMap, context: Context) {

    private var mMap: GoogleMap? = null
    private var context: Context? = null
    // Se almacenara todos los marcadores que generara el usuario
    private var listaMarcadores: ArrayList<Marker>? = null

    // Guardara la ruta para que posteriormente se pueda actualizar
    private var rutaMarcada: Polyline? = null
    private var marker: MarkerOptions? = null

    var miUbicacion: LatLng? = null

    init {
        this.mMap = mMap
        this.context = context
    }

    fun  dibujarLineas(){
        // Se establecen coordenadas para dibujar una linea
        val coordenadas = PolylineOptions()
            .add(LatLng(19.541894560478745, -96.92362068742396))
            .add(LatLng(19.55342048875402, -96.91409348112904))
            .add(LatLng(19.53412926046978, -96.91632507897947))
            .add(LatLng(19.529608077651993, -96.91181256784661))
            .pattern(arrayListOf<PatternItem>(Dot(), Gap(10.0F))) // Especificar el tipo de la distanciade en un patron
            .color(Color.CYAN)
            .width(15.0F) // ancho de la liena

        // Se establecen coordenadas para dibujar un Polygon
        val coordenadas2 = PolygonOptions()
            .add(LatLng(19.538263369998656, -96.90305783773776))
            .add(LatLng(19.547160850429506, -96.90546109698333))
            .add(LatLng(19.543601917111015, -96.90872266310231))
            .add(LatLng(19.539881130191077, -96.9107825995985))
            .strokePattern(arrayListOf<PatternItem>(Dash(10.0F), Gap(10.0F))) // Especificar el tipo de la distanciade en un patron
            .strokeColor(Color.YELLOW) // Color de la liena
            .fillColor(Color.GREEN) // Color de relleno
            .strokeWidth(15.0F) // Cambia el grosor de la liena

        // Se establecen coordenada y radio para dibujar un circulo
        val coordenadas3 = CircleOptions()
            .center(LatLng(19.56034439665008, -96.90760686416687))
            .radius(450.0)
            .strokePattern(arrayListOf<PatternItem>(Dash(10.0F), Gap(10.0F))) // Especificar el tipo de la distanciade en un patron
            .strokeColor(Color.MAGENTA) // Color de la liena
            .fillColor(Color.WHITE) // Color de relleno
            .strokeWidth(15.0F) // Cambia el grosor de la liena

        // Se agrega al mapa las coordenadas de la Polyline
        mMap?.addPolyline(coordenadas)
        // Se agrega al mapa las coordenadas de la Polygon
        mMap?.addPolygon(coordenadas2)
        // Se agrega al mapa la cordenada como punto incial para dibujar el circulo
        mMap?.addCircle(coordenadas3)
    }

    fun cambiarEstiloMapa(){
        val exitoCambioMapa = mMap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(
            context, R.raw.estilo_mapa))

        if (!exitoCambioMapa!!){
            // Mencionar que ocurrio un problema al cambiar el tipo de mapa
        }
    }

    /**
     * Añade marcadores cuando detecta un clcik prolongado y setea las coordenadas de donde presiono
     * el usuario
     */
     fun prepararMarcadores(){
        listaMarcadores = ArrayList()

        mMap?.setOnMapLongClickListener {
                location: LatLng ->
            mMap?.clear() // Limpia los elmentos del mapa
            listaMarcadores?.add(mMap?.addMarker(
                MarkerOptions()
                .position(location!!)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                .snippet("Esta es mi ciudad")
                .title("Titulo"))!!)
            // Obtiene el ultimo marcador que esta actualmente configurando el usuario
            listaMarcadores?.last()!!.isDraggable = true

            val coordenadas = LatLng(
                listaMarcadores?.last()!!.position.latitude,
                listaMarcadores?.last()!!.position.longitude)

            val origen = "origin=${miUbicacion?.latitude},${miUbicacion?.longitude}&"
            var destino = "destination=${coordenadas?.latitude },${coordenadas?.longitude}&"
            val parametros ="${origen}${destino}sensor=false&mode=driving&key=YOUR_API_KEY"
            Log.d("HTTP", "https://maps.googleapis.com/maps/api/directions/json?${parametros}")

            cargarUrl("https://maps.googleapis.com/maps/api/directions/json?${parametros}")
        }
    }

    /**
     * Realiza la solicitud a la Api de Google Maps
     */
    private fun cargarUrl(url: String){
        val queue = Volley.newRequestQueue(context)
        val solicitud = StringRequest(Request.Method.GET, url, Response.Listener<String> {

                response ->
            val coordenadas = obtenerCoordenadas(response)
            dibujarRuta(coordenadas)

        }, Response.ErrorListener {

        })
        queue.add(solicitud)
    }

    private fun dibujarRuta(coordenadas: PolylineOptions){
        if (rutaMarcada != null){
            rutaMarcada?.remove()
            listaMarcadores?.remove(listaMarcadores?.get(listaMarcadores?.size!! -1)) // Elimina el marcador guardado
        }
        Log.d("LISTAMARCADORES", listaMarcadores?.count().toString())
        rutaMarcada = mMap?.addPolyline(coordenadas)
    }

    /**
     * Metodo para castear la respuesta a la Api de Google maps
     */
    private fun obtenerCoordenadas(json: String): PolylineOptions {
        val gson = Gson()
        val objeto = gson.fromJson(json, ResponseModel::class.java)

        val puntos = objeto.routes?.get(0)!!.legs?.get(0)!!.steps!!

        var coordenadas = PolylineOptions()

        for (punto in puntos){
            coordenadas.add(punto.start_location?.toLatLng())
            coordenadas.add(punto.end_location?.toLatLng())
        }
        coordenadas
            .color(Color.CYAN)
            .width(15F)
        return coordenadas
    }

    @SuppressLint("MissingPermission")
    fun configurarMiUbicacion(){
        // Las dos siguientes lineas implementan el boton de localizacion para detectar la ubicacion
        mMap?.isMyLocationEnabled = true
        mMap?.uiSettings?.isMyLocationButtonEnabled = true
    }

    fun anadirMarcadorMiPosicion(){
        mMap?.addMarker(MarkerOptions().position(miUbicacion!!).title("Mi ubicación"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(miUbicacion))
    }

    fun ubicacionMiciudad(){
        // Asignacion de coordenadas de una ciudad
        val xalapaCity = LatLng(19.5426, -96.9137)

        // Se agrega un marcador con las coordenadas de la ciudad
        mMap?.addMarker(MarkerOptions().position(xalapaCity).title("Mi ciudad"))

        // Se posiciona la camara deacuerdo a las coordenadas de la ciudad con un zoom de 14 y
        // una animacion de 3 segundos.
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(xalapaCity, 14f), 3000, null)
    }
}