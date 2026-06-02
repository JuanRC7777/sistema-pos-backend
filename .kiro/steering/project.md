# Project Steering — Sistema POS

## Stack
- **Language:** Java 21
- **Runtime:** AWS Lambda con SnapStart
- **Database:** Amazon DynamoDB (2 tablas: pos-productos, pos-ventas)
- **Auth:** AWS Cognito (USER_PASSWORD_AUTH)
- **API:** API Gateway HTTP API con JWT Authorizer
- **IaC:** AWS SAM (template.yaml)
- **Build:** Maven + maven-shade-plugin (uber JAR)
- **Tests:** JUnit 5 + Mockito + AssertJ

## Architecture Pattern
Hexagonal (Ports & Adapters). El dominio es Java puro sin dependencias AWS. Los handlers Lambda son adapters primarios, los repositorios DynamoDB son adapters secundarios. No hay Spring ni ningún framework de inyección.

## Package Structure
```
com.pos.domain          → modelos y reglas de negocio puras
com.pos.application     → use cases, ports, DTOs, services
com.pos.infrastructure  → lambda handlers, dynamo repos, config
```

## Conventions
- Los servicios de aplicación reciben sus dependencias por constructor (DI manual).
- Los handlers leen variables de entorno en el constructor (cold start único).
- Todos los cálculos monetarios usan `BigDecimal` con `RoundingMode.HALF_UP`.
- Las fechas se almacenan en UTC en formato ISO-8601.
- DynamoDB usa Single-Table Design por tabla (PK/SK con prefijos).

## Deployed Resources (us-east-1)
- API URL: https://yv2kjdpi1k.execute-api.us-east-1.amazonaws.com/prod
- Cognito User Pool: us-east-1_ZxsTnOaDm
- Cognito Client: 12qfj6lsf16imccpjv0mq2gc44
- Stack: sistema-pos
