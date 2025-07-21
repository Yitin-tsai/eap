# EAP Common Module

This module contains shared code and constants used across EAP microservices.

## Features

### RabbitMQ Constants
- Queue names
- Exchange names
- Routing keys

## Usage

Add the following dependency to your build.gradle:

```gradle
dependencies {
    implementation project(':eap-common')
}
```

Example usage:

```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(RabbitMQConstants.ORDER_CREATED_QUEUE);
    }
}
```

## Development

When making changes to constants, consider:
1. Backward compatibility
2. Impact on other services
3. Version management

## Testing

Run tests using:
```bash
./gradlew :eap-common:test
```
