package pl.ciruk.filmbag.integration

import org.testcontainers.containers.GenericContainer
import org.testcontainers.mariadb.MariaDBContainer

class KMariaDbContainer : MariaDBContainer("mariadb:11.4")
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
