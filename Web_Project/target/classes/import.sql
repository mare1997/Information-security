server.port: 8081

spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.password=root
spring.datasource.url=jdbc:mysql://localhost:3306/ibproject?useSSL=false
spring.datasource.username=root

dataDir=files

spring.jpa.hibernate.ddl-auto = create

logging.level.org.hibernate.SQL=DEBUG

spring.http.multipart.max-file-size=1MB
spring.http.multipart.max-request-size=1MB
spring.data.elasticsearch.properties.http.enabled=true

server.ssl.keyStoreType: PKCS12
server.ssl.key-store=data/spring.keystore
server.ssl.key-store-password=spring