name := "Diffa"

version := "1.0"

scalaVersion := "2.9.1"

resolvers += "LShift Nexus" at "http://nexus.lshift.net:8080/nexus/content/groups/public"

libraryDependencies ++= {
  val springRelease = "3.1.0.RELEASE"
  val apacheVersion = "1.5.5"
  val hibernateVersion = "3.6.7-2L"
  val jacksonVersion = "1.9.2"
  val jerseyVersion = "1.10"
  val slf4jVersion = "1.6.4"
  val wrojVersion = "1.4.4"
  Seq(
    "cglib" % "cglib-nodep" % "2.1_3",
    "com.sun.jersey" % "jersey-server" % jerseyVersion,
    "com.sun.jersey" % "jersey-client" % jerseyVersion,
    "com.sun.jersey.contribs" % "jersey-spring" % jerseyVersion excludeAll(
      ExclusionRule(organization = "org.springframework"),
      ExclusionRule(organization = "commons-logging")
    ),
    "net.sf.ehcache" % "ehcache-core" % "2.4.7",
    "net.sf.opencsv" % "opencsv" % "2.3",
    "org.antlr" % "stringtemplate" % "3.2.1",
    "org.apache.directory.server" % "apacheds-core" % apacheVersion,
    "org.apache.directory.server" % "apacheds-protocol-ldap" % apacheVersion,
    "org.aspectj" % "aspectjweaver" % "1.6.8",
    "org.codehaus.castor" % "castor-xml" % "1.3.1" exclude("commons-logging", "commons-logging"),
    "org.codehaus.jackson" % "jackson-xc" % jacksonVersion,
    "org.codehaus.jackson" % "jackson-mapper-asl" % jacksonVersion,
    "org.hibernate" % "hibernate-core" % hibernateVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
    "org.springframework" % "spring-core" % springRelease exclude("commons-logging", "commons-logging"),
    "org.springframework" % "spring-web" % springRelease,
    "org.springframework" % "spring-aop" % springRelease,
    "org.springframework" % "spring-orm" % springRelease,
    "org.springframework" % "spring-oxm" % springRelease,
    "org.springframework" % "spring-expression" % springRelease,
    "org.springframework" % "spring-tx" % springRelease,
    "org.springframework" % "spring-jdbc" % springRelease,
    "org.springframework" % "spring-test" % springRelease,
    "org.springframework.security" % "spring-security-web" % springRelease,
    "org.springframework.security" % "spring-security-config" % springRelease,
    "org.springframework.security" % "spring-security-taglibs" % springRelease,
    "org.springframework.security" % "spring-security-ldap" % springRelease,
    "ro.isdc.wro4j" % "wro4j-core" % wrojVersion exclude("org.slf4j", "slf4j-log4j12"),
    "ro.isdc.wro4j" % "wro4j-extensions" % wrojVersion exclude("org.slf4j", "slf4j-log4j12")
  )
}
