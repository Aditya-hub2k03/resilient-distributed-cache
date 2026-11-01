Alteration and addition in pom.xml

<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>

addition in application.properties

"

`#Logger`

`logging.config=classpath:log4j2-spring.xml`

"

creating a new file in resources with the name "log4j2-spring.xml" with the contents 

<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" monitorInterval="30">

​    <Properties>
​        <Property name="LOG_DIR">logs</Property>
​        <Property name="LOG_PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n%throwable</Property>
​    </Properties>

​    <Appenders>

​        <Console name="Console" target="SYSTEM_OUT">
​            <PatternLayout pattern="${LOG_PATTERN}" charset="UTF-8"/>
​        </Console>

​        <RollingFile name="FileLogger"
​                     fileName="${LOG_DIR}/selfheal-dc-${date:yyyy-MM-dd_HH-mm-ss-SSS}.log"
​                     filePattern="${LOG_DIR}/selfheal-dc-%d{yyyy-MM-dd_HH-mm-ss-SSS}.log">

​            <PatternLayout pattern="${LOG_PATTERN}" charset="UTF-8"/>

​            <Policies>
​                <OnStartupTriggeringPolicy/>
​                <SizeBasedTriggeringPolicy size="20MB"/>
​            </Policies>

​            <DefaultRolloverStrategy max="100"/>

​        </RollingFile>

​    </Appenders>

​    <Loggers>

​        <Logger name="com.yp.selfheal.dc"
​                level="DEBUG"
​                additivity="false">
​            <AppenderRef ref="Console"/>
​            <AppenderRef ref="FileLogger"/>
​        </Logger>

​        <Logger name="org.springframework" level="INFO"/>
​        <Logger name="org.hibernate" level="INFO"/>
​        <Logger name="com.zaxxer.hikari" level="DEBUG"/>
​        <Logger name="com.mysql" level="INFO"/>
​        <Logger name="org.apache.catalina" level="INFO"/>
​        <Logger name="io.lettuce" level="DEBUG"/>
​        <Logger name="org.apache.kafka" level="DEBUG"/>

​        <Root level="DEBUG">
​            <AppenderRef ref="Console"/>
​            <AppenderRef ref="FileLogger"/>
​        </Root>

​    </Loggers>

</Configuration>