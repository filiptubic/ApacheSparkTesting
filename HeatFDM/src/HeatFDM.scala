/**
  * Created by filiptubic on 10/27/16.
  */

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD
import org.apache.log4j.{Level, Logger}

object HeatFDM {
  val size = 100
  val N = 2000
  val k = 1.0d / (size - 1.0d)
  val h = 0.21
  val REDUCTION_INTERVAL = 100

  val r = k / (h * h)
  var points: Array[(Int, Double)] = Array()
  var data: RDD[(Int, Double)] = _

  def interior(ix: (Int, Double)): Boolean = {
    if (ix._1 > 0 && (ix._1 < (size -1))) true
    else false
  }

  def stencil(x: (Int, Double)) = {
    var arr = Array((x._1, (1 - 2 * r) * x._2))
    arr :+= (x._1 - 1, r * x._2)
    arr :+= (x._1 + 1, r * x._2)
    arr = arr.filter(elem => interior(elem))
    arr
  }

  def main(args: Array[String]): Unit = {

    //Conf, Context
    val conf = new SparkConf()
      .setAppName("FDM Heat Equation")
      .setMaster("local")

    val sc = new SparkContext(conf)
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    //FDM Heat parallel
    points :+= (0, 0.0d)
    for (i <- 1 to size-2) {
      points :+= (i, 100.0d * Math.sin(Math.PI * i * k))
    }
    
    points :+= (size, 0.0d)
    data = sc.parallelize(points)

    for (i <- 1 to N) {
      val stencilParts = data.flatMap(x => stencil(x))
      data = stencilParts.reduceByKey((x, y) => x + y)
      if (i % REDUCTION_INTERVAL == 0)
        data.collect()
    }

    println("(0,0.0)")
    data.sortBy(x => x._1, true).foreach(println)
    println("("+ (size - 1) + ",0.0)")
  }
}