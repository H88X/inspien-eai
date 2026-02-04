plugins {
    id("java")
}

group = "com.inspien.eai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Oracle JDBC
    implementation("com.oracle.database.jdbc:ojdbc11:21.9.0.0")
    // SFTP
    implementation("com.jcraft:jsch:0.1.55")
    // XML
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.3")
    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")

    // XML Parsing
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.3")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

}

tasks.test {
    useJUnitPlatform()
}