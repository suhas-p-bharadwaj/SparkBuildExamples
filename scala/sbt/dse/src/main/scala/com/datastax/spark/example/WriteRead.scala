package com.datastax.spark.example

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.cassandra._
import java.util.Properties

// For DSE it is not necessary to set connection parameters for spark.master (since it will be done
// automatically)
object WriteRead extends App {

  val spark = SparkSession.builder
    .appName("Datastax Scala Pref export example")
    .enableHiveSupport()
    .getOrCreate()

  import spark.implicits._
  
  val connectionProperties = new Properties()

  val sql = s"( select u.login as login, 'defaultTerr' as prefType, NVL(TO_CHAR(t1.derived_segment1),'') as pref_val from fda_users u, fda_territory t1  where u.active_flag = 'Y' and u.default_terr_key = t1.terr_key )"

  val jdbcUrl = s"jdbc:oracle:thin:FDATERR/ispfda07@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=dbc-stg-2095-vip.cisco.com)(PORT=1532))(CONNECT_DATA=(SERVICE_NAME=ESALESQA.CISCO.COM)(Server=Dedicated)))"

  val prefTable = spark.read.jdbc(jdbcUrl, sql, connectionProperties)

  val mapPref = prefTable.map( row => (row.getString(0), row.getString(1), row.getString(2)) )

  val mapPrefRdd = mapPref.rdd

  mapPrefRdd.saveToCassandra("ngfcstc","userpreftable", SomeColumns("login","pref_type","pref_val"))
  

  spark.stop()
  sys.exit(0)
}