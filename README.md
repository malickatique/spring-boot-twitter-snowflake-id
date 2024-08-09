# Snowflake ID Generator

A unique ID generator implementation using the Snowflake algorithm, designed for Spring Boot applications. This implementation is based on Twitter's Snowflake algorithm and generates 64-bit unique identifiers with high performance and scalability.

## Features

- **Unique ID Generation**: Produces unique 64-bit IDs.
- **Scalable**: Supports high-performance ID generation for distributed systems.
- **Timestamp-Based**: Ensures IDs are ordered chronologically.
- **Configurable**: Allows configuration of datacenter and machine IDs.

## Requirements

- Java 8 or later
- Spring Boot

## Installation

Add the `SnowflakeIdGenerator` class to your Spring Boot project. Ensure you have a configuration class or component (`EurekaConfig` in this case) to provide datacenter and machine IDs.

## Usage

1. **Add Dependency**: Ensure your Spring Boot application includes the `SnowflakeIdGenerator` class.

2. **Configuration**: Create or update your `EurekaConfig` class to provide the necessary datacenter and machine IDs.

   ```java
   @Configuration
   public class EurekaConfig {
       public long getCurrentDataCenterIn5Bit() {
           // Return your datacenter ID (0 to 31)
       }

       public long getCurrentInstanceIndex() {
           // Return your machine ID (0 to 31)
       }
   }
   ```
3. **Create Bean**: Register the SnowflakeIdGenerator as a Spring bean.

   ```java
    @Configuration
    public class AppConfig {
        @Bean
        public SnowflakeIdGenerator snowflakeIdGenerator(EurekaConfig eurekaConfig) {
            return new SnowflakeIdGenerator(eurekaConfig);
        }
    }
   ```

4. **Generate IDs**: Use the SnowflakeIdGenerator bean to generate unique IDs.

   ```java
    @Autowired
    private SnowflakeIdGenerator idGenerator;

    public void someMethod() {
        long uniqueId = idGenerator.generateId();
        // Use the unique ID
    }
   ```

## How It Works
- **Epoch**: The epoch is set to Monday, January 1, 2024, 12:00:00 AM. This is the start time for ID generation.
- **ID Structure**: The generated ID consists of:
    - A timestamp (milliseconds since epoch)
    - Datacenter ID (5 bits)
    - Machine ID (5 bits)
    - Sequence number (12 bits)
    - Sequence Overflow Handling: If the sequence number overflows within the same millisecond, the generator waits for the next millisecond.

## Error Handling
- **Clock Moves Backwards**: Throws an exception if the system clock moves backwards.
- **Interrupted Exception**: Handles interruptions during the wait for the next millisecond.

## Contributing:
Contributions are welcome! Please open an issue or submit a pull request if you have improvements or bug fixes.
