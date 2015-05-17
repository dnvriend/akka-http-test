package com.github.dnvriend

import java.text.SimpleDateFormat
import java.util.Date

object TimeUtil {
  def timestamp: String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    sdf.format(new Date)
  }
}
