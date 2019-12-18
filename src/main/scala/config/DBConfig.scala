package config

class DBConfig {
  val dbDriver = "org.h2.Driver"
  val dbUrl    = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
  val dbUser   = "sa"
  val dbPass   = ""
}
