package com.technogi.microservices.mxpostalcodes

object DataLoader {

    fun loadData(): Map<Int, List<Location>> {
        val codesMap = mutableMapOf<Int, List<Location>>()

        DataLoader::class.java.classLoader.getResource("CPdescarga.txt")
                .openStream()
                .bufferedReader(charset("ISO-8859-1"))
                .lines()
                .forEach { line ->
                    val data = line.split("|")
                    if (data.size == 15) {
                        try {
                            val cp = Integer.parseInt(data[Location.CODIGO_POSTAL])
                            val newVal = listOf(
                                    Location(
                                            data[Location.ASENTAMIENTO],
                                            data[Location.MUNICIPIO],
                                            data[Location.ESTADO],
                                            data[Location.CIUDAD],
                                            data[Location.ID_ESTADO],
                                            data[Location.ID_MUNICIPIO]))

                            if (codesMap.containsKey(cp)) {
                                val oldVal = codesMap.get(cp)
                                codesMap.replace(cp, newVal + oldVal!!)
                            } else {
                                codesMap.put(cp, newVal)
                            }
                        } catch (e: NumberFormatException) {
                            //IGNORE line
                        }
                    }
                }
        return codesMap
    }
}