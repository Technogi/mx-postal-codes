package com.technogi.microservices.mxpostalcodes

data class Location(val asentamiento: String, val municipio: String, val estado: String, val ciudad: String, val idEstado: String, val idMunicipio: String, val idCiudad: String) {
    //d_codigo|d_asenta|d_tipo_asenta|D_mnpio|d_estado|d_ciudad|d_CP
    companion object {
        const val CODIGO_POSTAL = 0
        const val ASENTAMIENTO = 1 // Colonia
        const val MUNICIPIO = 3
        const val ESTADO = 4
        const val CIUDAD = 5
        const val ID_ESTADO = 7
        const val ID_MUNICIPIO = 11
        const val ID_CIUDAD = 14
    }
}