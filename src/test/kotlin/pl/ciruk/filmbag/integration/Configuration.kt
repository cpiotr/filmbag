package pl.ciruk.filmbag.integration

import org.hibernate.dialect.MariaDB10Dialect

class Utf8MariaDbDialect : MariaDB10Dialect() {
    override fun getTableTypeString() = " DEFAULT CHARSET=utf8"
}