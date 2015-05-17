package com.github.dnvriend

/**
 * {
 *   "coord":{"lon":5.21,"lat":52.37},
 *   "sys":{"message":0.2418,"country":"NL","sunrise":1431833997,"sunset":1431891067},
 *   "weather":[{"id":802,"main":"Clouds","description":"scattered clouds","icon":"03d"}],
 *   "base":"stations",
 *   "main":{"temp":287.345,"temp_min":287.345,"temp_max":287.345,"pressure":1036.1,"sea_level":1037,"grnd_level":1036.1,"humidity":72},
 *   "wind":{"speed":6.17,"deg":276.5},
 *   "clouds":{"all":32},
 *   "dt":1431869166,
 *   "id":0,
 *   "name":"Kruidenwijk, Staatsliedenwijk",
 *   "cod":200
 *   }
 */
case class WeatherResult(
                          coord: Coord,
                          sys: Sys,
                          weather: List[Weather],
                          base: String,
                          main: Main,
                          wind: Wind,
                          clouds: Cloud,
                          dt: Long,
                          id: Int,
                          name: String,
                          cod: Int
                          )

case class Wind(speed: Double, deg: Double)
case class Main(temp: Double, temp_min: Double, temp_max: Double, pressure: Double, sea_level: Double, grnd_level: Double, humidity: Int)
case class Cloud(all: Int)
case class Weather(id: Int, main: String, description: String, icon: String)
case class Sys(message: Double, country: String, sunrise: Long, sunset: Long)
case class Coord(lon: Double, lat: Double)