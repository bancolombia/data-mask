# Data Masking Utility

[code-of-conduct]: CODE_OF_CONDUCT.md
[contributing]: CONTRIBUTING.md
[encryption_context]: https://aws.amazon.com/blogs/security/how-to-protect-the-integrity-of-your-encrypted-data-by-using-aws-key-management-service-and-encryptioncontext/

Utility library to use with Jackson-Databind to provide custom
POJO/JSON serialization and deserialization aiming to protect
sensitive data via masking with additional encrypting-decrypting.

Functionality:

Using a customized Object Mapper you can:

- Perform Masking on string members of an object

  - Scenario: masking a credit card number when serializing to json.

    ```java
    public class Customer {     
    
        @Mask(rightVisible=4)
        public String creditCardNumber;
    
    }
    
    Customer c = new Customer();
    c.creditCardNumber = "1111222233334444";
    String json = objectMapper.writeValueAsString(c);
    
    assert json.equals("{ \"creditCardNumber\": \"***************4444\"}")
    ```

- Encrypting:

  Converting string members of an object into a pair of masked an ecrypted values.

  - As an composite String:

    ```java
    public class Customer {     
    
        @Mask(rightVisible=4, queryOnly=false)
        public String creditCardNumber;
    
    }

    Customer c = new Customer();
    c.creditCardNumber = "1111222233334444";
    String json = objectMapper.writeValueAsString(c);
    
    assert json.equals("{
      \"creditCardNumber\": \"masked_pair=***************4444|<credit card encrypted value>\"
    }")
    ```

  - Or as Json Object.

    ```java
    public class Customer {     
    
        @Mask(rightVisible=4, queryOnly=false, format=DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        public String creditCardNumber;
    
    }

    Customer c = new Customer();
    c.creditCardNumber = "1111222233334444";
    
    String json = objectMapper.writeValueAsString(c);
    
    assert json.equals("{
      \"creditCardNumber\": {
        \"masked\": \"***************4444\",
        \"enc\": \"<credit card encrypted value>\"
      }
    }")
    ```


- Decrypting: Reverting an encrypted string/json input value to its original plain value.

  - Having a JSON with a composite string value:

    ```java
    String json = "{
      \"creditCardNumber\": \"masked_pair=***************4444|<credit card encrypted value>\"
    }";
    
    Customer c = objectMapper.readValue(json, Customer.class);
    
    assert c.creditCardNumber.equals("1111222233334444");
    ```

  - Having a JSON with an Object value:

    ```java
    String json = "{
      \"creditCardNumber\": {
        \"masked": \"***************4444\",
        \"enc": \"<credit card encrypted value>\"
      }
    }";
  
    Customer c = objectMapper.readValue(json, Customer.class);
    
    assert c.creditCardNumber.equals("1111222233334444");
    ```

## Installing

With Gradle
```gradle
implementation 'com.github.bancolombia:data-mask-core:1.0.1'
```

With maven
```maven
<dependency>
  <groupId>com.github.bancolombia</groupId>
  <artifactId>data-mask-core</artifactId>
  <version>1.0.1</version>
</dependency>
```

This library depends on:

- `org.apache.commons:commons-lang3`
- `com.fasterxml.jackson.core:jackson-databind`

## Using Data-Mask

### A. Implement Cipher and Decipher interfaces

This library defines two interfaces: `DataCipher` and `DataDecipher` which are used in the encryption/decryption
processes.

User of this library must define implementation for both interfaces.

Dummy Example:
```java
var dummyCipher = new DataCipher() {
    @Override
    public String cipher(String plainData) {
        return "the encrypted value";
    }
};

var dummyDecipher = new DataDecipher() {
    @Override
    public String decipher(String encryptedData) {
        return "the plain value";
    }
};

```

### B. Declare the customized Object Mapper.

This library defines a custom `ObjectMapper` in order to provide the masking and unmasking functionality, and takes
as constructor arguments, the implementations of both `DataCipher` and `DataDecipher` interfaces.

```java
    public ObjectMapper objectMapper(DataCipher someCipherImpl, DataDecipher someDecipherImpl) {
        return new MaskingObjectMapper(someCipherImpl, someDecipherImpl);
    }
```

### C. Decorate POJO's

Members to be masked/encrypted should be annotated with `@Mask`, eg:

```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4)
        private String email;

        @Mask(rightVisible=4, queryOnly=false, format=DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String creditCardNumber;
    }
````

__Anotation Properties__

| Attribute  | Default value  | Description  |
|---|---|---|
| leftVisible  | 0  | Masking: how many characters should remain visible on left. Example: Hello******  |
| rightVisible  | 4  | Masking: how many characters should remain visible on right Example: *****World |
| queryOnly  | true  | `true`: Serialization should generate masked value only. |
|  |  |  `false`: serialization should generate masked value and encrypted value. |
| format | ENCRYPTION_AS_OBJECT | Describes how masked and encrypted data should be serialized. Using `ENCRYPTION_INLINE` means masked and encrypted values together are serialized as string: ```masked_pair=<masked_value>I<encrypted_value>``` |
|  |  | Using `ENCRYPTION_AS_OBJECT`, means masked and encrypted values are serialized as a json object. |


### D. Use Custom ObjectMapper

Use the custom `ObjectMapper`, so, having this example of annotated class:

```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Customer {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4)
        private String email;

        @Mask(rightVisible=4, queryOnly=false, format=DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String creditCardNumber;
    }
````

__Serializing Process__

An instance of example `Customer` annotated class:

```java
    Customer customer = new Customer("Jhon Doe", 
        "jhon.doe123@someservice.com", 
        "4444555566665678");
```

Should be serialized as JSON like this:

```java
String json = mapper.writeValueAsString(customer);
```

```json
{
  "name": "Jhon Doe",
  "email": "Jho**************.com",
  "creditCardNumber": {
    "masked": "************5678",
    "enc": "dGhpcyBzaG91bGQgYmUgYW4gZWNyeXB0ZWQgdmFsdWUK"
  }
}
```

__Deserializing Process__

The deserialization process should construct an instance of the example `Customer`
with is `creditCardNumber`property in plain text.

```java
String json = "{\n" +
    "\"name\": \"Jhon Doe\",\n" +
    "\"email\": \"Jho**************.com\",\n" +
    "\"creditCardNumber\": {\n" +
    "\"masked\": \"************5678\",\n" +
    "\"enc\": \"dGhpcyBzaG91bGQgYmUgYW4gZWNyeXB0ZWQgdmFsdWUK\"\n" +
    "}\n" +
    "}";

Customer customer = mapper.readValue(json, Customer.class);
assertEquals("4444555566665678", customer.creditCardNumber());
```

### E. Using library without POJO model

- **Transformation of Json**


The library offers a funtionability for transform JSON without a model known. The transformations supported are Cyphering, Decyphering and Masking.

**DISCLAIMER**: This require high computer resources because it looping over JSON searching specific fields that we configurate.

- *How to configure specific field for cypher from JSON?*

![](https://i.imgur.com/cYmrxtI.png)


- *How to configure specific field for masking from JSON?*

![](https://i.imgur.com/7ZYWmPu.png)


- How to decypher a JSON previously cyphered?

![](https://i.imgur.com/insoc94.png)

**1)** Transforming *obj(Any Json)* with specific configuration explained in previos answers

**2)** Getting original *obj* previosly transformed


- Can I use cyphering and masking in only one search?

![](https://i.imgur.com/SeiaFYj.png)




# AWS SDK integration

This library offers a concrete implementation for the `DataCipher` and `DataDecipher` interfaces
called `data-mask-aws` which provides via the **Aws crypto SDK** and Secrets Manager
the encryption and decryption funcionality.

## Using

With Gradle
```gradle
implementation 'com.github.bancolombia:data-mask-aws:1.0.1'
```

With maven
```maven
<dependency>
  <groupId>com.github.bancolombia</groupId>
  <artifactId>data-mask-aws</artifactId>
  <version>1.0.1</version>
</dependency>
```

### Additional configuration

Passed via configuration `application.properties` or `application.yaml`

| Attribute  | Default value  | Description  |
|---|---|---|
| secrets.dataMaskKey  |  | Name of the stored symmetric key in AWS Secrets manager |
| dataMask.encryptionContext | "default_context" | The context for additional protection of the encrypted data. See [Usage of Encryption contexts][encryption_context].|
| adapters.aws.secrets-manager.region  |   | Region for the Secrets Manager service  |
| adapters.aws.secrets-manager.endpoint  |   | (Optional) for local dev only|

### Use with Spring-Boot

Just declare the customized Object Mapper as a Bean, and add **@Primary** annotation to use instead of the default ObjectMapper.

```java
    @Bean
@Primary
public ObjectMapper objectMapper(DataCipher awsCipher, DataDecipher awsDecipher) {
        return new MaskingObjectMapper(awsCipher, awsDecipher);
        }
```

# Contribute

Please read our [Code of conduct][code-of-conduct] and [Contributing Guide][contributing]. 

