package uk.gov.dluhc.printapi.config

import org.testcontainers.containers.MySQLContainer

class MySQLContainerConfiguration : MySQLContainer<MySQLContainerConfiguration>(MYSQL_IMAGE) {
    companion object {
        private const val MYSQL_IMAGE = "mysql:8"
        private const val DATABASE = "print_application"
        private const val USER = "root"
        private const val PASSWORD = "password"
        private const val DATASOURCE_URL = "spring.datasource.url"
        private var container: MySQLContainerConfiguration? = null

        fun getInstance(): MySQLContainerConfiguration {
            if (container == null) {
                container = MySQLContainerConfiguration().withDatabaseName(DATABASE)
                    .withUsername(USER)
                    .withPassword(PASSWORD)
                    .withReuse(true)
                    .withCreateContainerCmdModifier {
                        it.withName("print-api-mysql")
                    }
                    .also {
                        it.start()
                        System.setProperty(DATASOURCE_URL, it.jdbcUrl)
                    }
            }
            return container!!
        }
    }
}
