package pl.ciruk.filmbag.integration

import org.hibernate.dialect.MariaDB10Dialect
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer

class Utf8MariaDbDialect : MariaDB10Dialect() {
    override fun getTableTypeString() = " DEFAULT CHARSET=utf8"
}

class KMariaDbContainer : MariaDBContainer<KMariaDbContainer>("mariadb:10.6.3")
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
